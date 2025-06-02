package br.com.sankhya.truss.corte.helper;

import br.com.sankhya.dwf.services.ServiceUtils;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.mgecomercial.model.centrais.cac.CACSP;
import br.com.sankhya.mgecomercial.model.centrais.cac.CACSPHome;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;
import com.sankhya.util.XMLUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.Principal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.jdom.Content;
import org.jdom.Element;

public class CorteHelper {
	public String validaTopCorte(BigDecimal codtipoper) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
		try {
			String corte = null;
			NativeSql query = new NativeSql(jdbc);
			query.setNamedParameter("P_CODTIPOPER", codtipoper);
			ResultSet r = query.executeQuery("SELECT NVL(AD_CORTE,'N') AS CORTE FROM TGFTOP TPO WHERE TPO.CODTIPOPER = :P_CODTIPOPER AND TPO.DHALTER = (SELECT MAX(T.DHALTER) FROM TGFTOP T WHERE T.CODTIPOPER = TPO.CODTIPOPER)");
			while (r.next())
				corte = r.getString("CORTE");
			return corte;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Falha ao validar TOP de Corte.\n" + e.getMessage());
		}
	}

	public static String validaTopIndicaLote(BigDecimal codtipoper) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
		try {
			jdbc.openSession();
			String indicaLote = null;
			NativeSql query = new NativeSql(jdbc);
			query.setNamedParameter("P_CODTIPOPER", codtipoper);
			ResultSet r = query.executeQuery("SELECT NVL(AD_INDICALOTE,'N') AS INDICALOTE FROM TGFTOP TPO WHERE TPO.CODTIPOPER = :P_CODTIPOPER AND TPO.DHALTER = (SELECT MAX(T.DHALTER) FROM TGFTOP T WHERE T.CODTIPOPER = TPO.CODTIPOPER)");
			while (r.next())
				indicaLote = r.getString("INDICALOTE");
			return indicaLote;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Falha ao validar TOP de Corte.\n" + e.getMessage());
		} finally {
			jdbc.closeSession();
		}
	}

	public static BigDecimal buscaTopCorte(DynamicVO cabVO, JdbcWrapper jdbc) throws Exception {
		try {
			BigDecimal codtipoper = cabVO.asBigDecimal("CODTIPOPER");
			Timestamp dhtipoper = null;
			NativeSql q = new NativeSql(jdbc);
			q.setNamedParameter("P_CODTIPOPER", cabVO.asBigDecimal("CODTIPOPER"));
			ResultSet r = q.executeQuery("SELECT MAX(DHALTER) AS DHALTER FROM TGFTOP WHERE CODTIPOPER = :P_CODTIPOPER");
			while (r.next())
				dhtipoper = r.getTimestamp("DHALTER");
			JapeWrapper topDAO = JapeFactory.dao("TipoOperacao");
			DynamicVO topVO = topDAO.findByPK(new Object[] { codtipoper, dhtipoper });
			BigDecimal topCorte = topVO.asBigDecimal("AD_TOPCORTEGLOBAL");
			if (topCorte == null || BigDecimal.ZERO.equals(topCorte))
				throw new Exception("Nexiste TOP de corte cadastrada para a TOP " + cabVO.asBigDecimal("CODTIPOPER"));
			return topCorte;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Falha ao buscar TOP de faturamento.\n" + e.getMessage());
		}
	}

	public static void indicaLotes(BigDecimal nunota) throws Exception {
		JapeWrapper iteDAO = JapeFactory.dao("ItemNota");
		JapeWrapper cabDAO = JapeFactory.dao("CabecalhoNota");
		Collection<DynamicVO> itesVO = iteDAO.find("NUNOTA = ?", new Object[] { nunota });
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
		try {
			((FluidUpdateVO)cabDAO.prepareToUpdateByPK(new Object[] { nunota }).set("AD_DESCONSCORTE", "S"))
					.update();
			for (DynamicVO iteVO : itesVO) {
				NativeSql query = new NativeSql(jdbc);
				query.setNamedParameter("P_CODPROD", iteVO.asBigDecimal("CODPROD"));
				query.setNamedParameter("P_CODEMP", iteVO.asBigDecimal("CODEMP"));
				query.setNamedParameter("P_CODLOCAL", iteVO.asBigDecimal("CODLOCALORIG"));
				ResultSet r = query.executeQuery("SELECT CODPROD, CONTROLE, NVL(DISPONIVEL,0) AS DISPONIVEL  FROM AD_VW_ESTOQUEPORPARCEIRO EST  WHERE CODPROD = :P_CODPROD  AND EST.CODEMP = :P_CODEMP  AND EST.CODLOCAL = :P_CODLOCAL  AND DISPONIVEL > 0  ORDER BY DTVAL ");
				BigDecimal qtdRestante = iteVO.asBigDecimal("QTDNEG");
				int count = 0;
				while (r.next()) {
					BigDecimal disponivel = r.getBigDecimal("DISPONIVEL");
					String controle = r.getString("CONTROLE");
					if (count == 0) {
						if (disponivel.compareTo(qtdRestante) >= 0) {
							iteVO.setProperty("CONTROLE", controle);
							dwfEntityFacade.saveEntity("ItemNota", (EntityVO)iteVO);
							break;
						}
						iteVO.setProperty("QTDNEG", disponivel);
						iteVO.setProperty("VLRTOT", disponivel.multiply(iteVO.asBigDecimal("VLRUNIT")));
						iteVO.setProperty("CONTROLE", controle);
						dwfEntityFacade.saveEntity("ItemNota", (EntityVO)iteVO);
						qtdRestante = qtdRestante.subtract(disponivel);
					} else {
						if (disponivel.compareTo(qtdRestante) >= 0) {
							insereItem(iteVO, qtdRestante, controle);
							break;
						}
						insereItem(iteVO, disponivel, controle);
						qtdRestante = qtdRestante.subtract(disponivel);
					}
					count++;
				}
				((FluidUpdateVO)cabDAO.prepareToUpdateByPK(new Object[] { nunota }).set("AD_DESCONSCORTE", "N"))
						.update();
			}
			recalculaNota(nunota);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Erro ao indicar Lotes: " + e.getMessage());
		} finally {
			jdbc.closeSession();
		}
	}

	public static void insereItem(DynamicVO iteVO, BigDecimal quantidade, String controle) throws Exception {
		JapeWrapper iteDAO = JapeFactory.dao("ItemNota");
		try {
			((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)iteDAO.create()
					.set("NUNOTA", iteVO.asBigDecimal("NUNOTA")))
					.set("CODPROD", iteVO.asBigDecimal("CODPROD")))
					.set("QTDNEG", quantidade))
					.set("CONTROLE", controle))
					.set("VLRUNIT", iteVO.asBigDecimal("VLRUNIT")))
					.set("VLRTOT", iteVO.asBigDecimal("VLRUNIT").multiply(quantidade)))
					.set("CODLOCALORIG", iteVO.asBigDecimal("CODLOCALORIG")))
					.set("CODVOL", iteVO.asString("CODVOL")))
					.set("ATUALESTOQUE", iteVO.asBigDecimal("ATUALESTOQUE")))
					.set("RESERVA", iteVO.asString("RESERVA")))
					.set("NUTAB", iteVO.asBigDecimal("NUTAB")))
					.save();
		} catch (Exception e) {
			throw new Exception("Erro ao incluir itens de lote: " + e.getMessage());
		}
	}

	public static void recalculaNota(BigDecimal nunota) throws Exception {
		JapeWrapper cabDAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO cabVO = cabDAO.findByPK(new Object[] { nunota });
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity persistentEntityCab = dwfEntityFacade.findEntityByPrimaryKey("CabecalhoNota", new Object[] { nunota });
		ImpostosHelpper imposto = new ImpostosHelpper();
		imposto.carregarNota(nunota);
		imposto.calculaICMS(true);
		imposto.totalizarNota(nunota);
		imposto.setForcarRecalculo(true);
		imposto.setAtualizaImpostos(true);
		imposto.setCalcularTudo(true);
		imposto.calcularImpostos(nunota);
		imposto.salvarNota();
		BigDecimal totalNota = imposto.calcularTotalNota(cabVO.asBigDecimal("NUNOTA"), imposto.calcularTotalItens(cabVO.asBigDecimal("NUNOTA"), false));
		cabVO.setProperty("VLRNOTA", totalNota);
		persistentEntityCab.setValueObject((EntityVO)cabVO);
	}

	public void alterarItem(DynamicVO iteVO, BigDecimal qtdneg) throws Exception {
		try {
			CACSP cacSP = (CACSP)ServiceUtils.getStatelessFacade("mge/com/ejb/session/CACSP", CACSPHome.class);
			ServiceContext ctx = new ServiceContext(null);
			ctx.setAutentication(AuthenticationInfo.getCurrent());
			ctx.makeCurrent();
			Element requestBody = ctx.getRequestBody();
			Element responseBody = ctx.getBodyElement();
			responseBody.removeContent();
			requestBody.removeContent();
			Element notaElem = new Element("nota");
			XMLUtils.addAttributeElement(notaElem, "NUNOTA", iteVO.asBigDecimal("NUNOTA"));
			Element itensElem = new Element("itens");
			Element itemElem = new Element("item");
			XMLUtils.addContentElement(itemElem, "NUNOTA", iteVO.asBigDecimal("NUNOTA"));
			XMLUtils.addContentElement(itemElem, "SEQUENCIA", iteVO.asBigDecimal("SEQUENCIA"));
			XMLUtils.addContentElement(itemElem, "CODPROD", iteVO.asBigDecimal("CODPROD"));
			XMLUtils.addContentElement(itemElem, "QTDNEG", qtdneg);
			XMLUtils.addContentElement(itemElem, "CONTROLE", iteVO.asString("CONTROLE"));
			XMLUtils.addContentElement(itemElem, "VLRUNIT", iteVO.asBigDecimal("VLRUNIT"));
			XMLUtils.addContentElement(itemElem, "VLRTOT", iteVO.asBigDecimal("VLRUNIT").multiply(qtdneg));
			XMLUtils.addContentElement(itemElem, "CODLOCALORIG", iteVO.asBigDecimal("CODLOCALORIG"));
			XMLUtils.addContentElement(itemElem, "CODVOL", iteVO.asString("CODVOL"));
			XMLUtils.addContentElement(itemElem, "PERCDESC", iteVO.asBigDecimal("PERCDESC"));
			XMLUtils.addContentElement(itemElem, "CALCULARDESCONTO", "S");
			XMLUtils.addContentElement(itemElem, "IGNORARRECALCDESC", "N");
			itensElem.addContent((Content)itemElem);
			notaElem.addContent((Content)itensElem);
			requestBody.addContent(notaElem.detach());
			cacSP.incluirAlterarItemNota(ctx);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Erro ao editar item do pedido.\n" + e.getMessage());
		}
	}

	public static ServiceContext createServiceContext() {
		ServiceContext ctx = new ServiceContext(new HttpServletRequest() {
			public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {}

			public void setAttribute(String arg0, Object arg1) {}

			public void removeAttribute(String arg0) {}

			public boolean isSecure() {
				return false;
			}

			public int getServerPort() {
				return 0;
			}

			public String getServerName() {
				return null;
			}

			public String getScheme() {
				return null;
			}

			public RequestDispatcher getRequestDispatcher(String arg0) {
				return null;
			}

			public String getRemoteHost() {
				return null;
			}

			public String getRemoteAddr() {
				return null;
			}

			public String getRealPath(String arg0) {
				return null;
			}

			public BufferedReader getReader() throws IOException {
				return null;
			}

			public String getProtocol() {
				return null;
			}

			public String[] getParameterValues(String arg0) {
				return null;
			}

			public Enumeration getParameterNames() {
				return null;
			}

			public Map getParameterMap() {
				return null;
			}

			public String getParameter(String arg0) {
				return "<root><requestBody></requestBody></root>";
			}

			public Enumeration getLocales() {
				return null;
			}

			public Locale getLocale() {
				return null;
			}

			public ServletInputStream getInputStream() throws IOException {
				return null;
			}

			public String getContentType() {
				return null;
			}

			public int getContentLength() {
				return 0;
			}

			public String getCharacterEncoding() {
				return null;
			}

			public Enumeration getAttributeNames() {
				return null;
			}

			public Object getAttribute(String arg0) {
				return null;
			}

			public boolean isUserInRole(String arg0) {
				return false;
			}

			public boolean isRequestedSessionIdValid() {
				return false;
			}

			public boolean isRequestedSessionIdFromUrl() {
				return false;
			}

			public boolean isRequestedSessionIdFromURL() {
				return false;
			}

			public boolean isRequestedSessionIdFromCookie() {
				return false;
			}

			public Principal getUserPrincipal() {
				return null;
			}

			public HttpSession getSession(boolean arg0) {
				return null;
			}

			public HttpSession getSession() {
				return null;
			}

			public String getServletPath() {
				return null;
			}

			public String getRequestedSessionId() {
				return null;
			}

			public StringBuffer getRequestURL() {
				return null;
			}

			public String getRequestURI() {
				return null;
			}

			public String getRemoteUser() {
				return null;
			}

			public String getQueryString() {
				return null;
			}

			public String getPathTranslated() {
				return null;
			}

			public String getPathInfo() {
				return null;
			}

			public String getMethod() {
				return null;
			}

			public int getIntHeader(String arg0) {
				return 0;
			}

			public Enumeration getHeaders(String arg0) {
				return null;
			}

			public Enumeration getHeaderNames() {
				return null;
			}

			public String getHeader(String arg0) {
				return null;
			}

			public long getDateHeader(String arg0) {
				return 0L;
			}

			public Cookie[] getCookies() {
				return null;
			}

			public String getContextPath() {
				return null;
			}

			public String getAuthType() {
				return null;
			}
		});
		ctx.makeCurrent();
		ctx.setAutentication(AuthenticationInfo.getCurrent());
		return ctx;
	}
}
