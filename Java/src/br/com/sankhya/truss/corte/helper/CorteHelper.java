package br.com.sankhya.truss.corte.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.Principal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jdom.Element;

import com.sankhya.util.XMLUtils;

import br.com.sankhya.dwf.services.ServiceUtils;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.mgecomercial.model.centrais.cac.CACSP;
import br.com.sankhya.mgecomercial.model.centrais.cac.CACSPHome;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class CorteHelper {
	
	public String validaTopCorte(BigDecimal codtipoper, JdbcWrapper jdbc) throws Exception {
		try {
			String corte = null;
			NativeSql query = new NativeSql(jdbc);
			query.setNamedParameter("P_CODTIPOPER", codtipoper);
			ResultSet r = query.executeQuery("SELECT NVL(AD_CORTE,'N') AS CORTE "
										   + "FROM TGFTOP TPO "
										   + "WHERE TPO.CODTIPOPER = :P_CODTIPOPER "
										   + "AND TPO.DHALTER = (SELECT MAX(T.DHALTER) FROM TGFTOP T WHERE T.CODTIPOPER = TPO.CODTIPOPER)");
			
			while(r.next()) {
				corte = r.getString("CORTE");
			}
			
			return corte;
		} catch(Exception e) {
			e.printStackTrace();
			throw new Exception("Falha ao validar TOP de Corte.\n" + e.getMessage());
		}
	}
	
	public static BigDecimal buscaTopCorte(DynamicVO cabVO, JdbcWrapper jdbc) throws Exception {
		try {
			BigDecimal codtipoper = cabVO.asBigDecimal("CODTIPOPER");
			Timestamp dhtipoper = null;
			
			NativeSql q = new NativeSql(jdbc);
			q.setNamedParameter("P_CODTIPOPER", cabVO.asBigDecimal("CODTIPOPER"));
			ResultSet r = q.executeQuery("SELECT MAX(DHALTER) AS DHALTER FROM TGFTOP WHERE CODTIPOPER = :P_CODTIPOPER");
			while(r.next()) {
				dhtipoper = r.getTimestamp("DHALTER");
			}
			
			JapeWrapper topDAO = JapeFactory.dao(DynamicEntityNames.TIPO_OPERACAO);
			
			DynamicVO topVO = topDAO.findByPK(codtipoper, dhtipoper);
			
			BigDecimal topCorte = topVO.asBigDecimal("AD_TOPCORTEGLOBAL");
			
			if(topCorte == null || BigDecimal.ZERO.equals(topCorte)) {
				throw new Exception("Não existe TOP de corte cadastrada para a TOP " + cabVO.asBigDecimal("CODTIPOPER"));
			}
			
			return topCorte;
			
		} catch(Exception e) {
			e.printStackTrace();
			throw new Exception("Falha ao buscar TOP de faturamento.\n" + e.getMessage());
		}
	}
	
	public void alterarItem(DynamicVO iteVO, BigDecimal qtdneg) throws Exception {
		try {	
				CACSP cacSP = (CACSP) ServiceUtils.getStatelessFacade(CACSPHome.JNDI_NAME, CACSPHome.class);
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
				
				
				itensElem.addContent(itemElem);
				notaElem.addContent(itensElem);
				requestBody.addContent(notaElem.detach());
				
				cacSP.incluirAlterarItemNota(ctx);
			} catch (Exception e){
				e.printStackTrace();
				throw new Exception("Erro ao editar item do pedido.\n" + e.getMessage());
			}
		}
	
	public static void recalculaNota(BigDecimal nunota) throws Exception {
		JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
		DynamicVO cabVO = cabDAO.findByPK(nunota);
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity persistentEntityCab = dwfEntityFacade.findEntityByPrimaryKey(DynamicEntityNames.CABECALHO_NOTA, new Object[] { nunota});
		
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
		
	@SuppressWarnings("rawtypes")
	public
	static ServiceContext createServiceContext() {
		ServiceContext ctx = new ServiceContext(new HttpServletRequest() {
			@Override
			public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException { }
			
			@Override
			public void setAttribute(String arg0, Object arg1) { }
			
			@Override
			public void removeAttribute(String arg0) { }
			
			@Override
			public boolean isSecure() { return false; }
			
			@Override
			public int getServerPort() { return 0; }
			
			@Override
			public String getServerName() { return null; }
			
			@Override
			public String getScheme() { return null; }
			
			@Override
			public RequestDispatcher getRequestDispatcher(String arg0) { return null; }
			
			
			@Override
			public String getRemoteHost() { return null; }
			
			@Override
			public String getRemoteAddr() { return null; }
			
			@Override
			public String getRealPath(String arg0) { return null; }
			
			@Override
			public BufferedReader getReader() throws IOException { return null; }
			
			@Override
			public String getProtocol() { return null; }
			
			@Override
			public String[] getParameterValues(String arg0) { return null; }
			
			@Override
			public Enumeration getParameterNames() { return null; }
			
			@Override
			public Map getParameterMap() { return null; }
			
			@Override
			public String getParameter(String arg0) { return "<root><requestBody></requestBody></root>"; }
			
			@Override
			public Enumeration getLocales() { return null; }
			
			@Override
			public Locale getLocale() { return null; }
			
			
			@Override
			public ServletInputStream getInputStream() throws IOException { return null; }
			
			@Override
			public String getContentType() { return null; }
			
			@Override
			public int getContentLength() { return 0; }
			
			@Override
			public String getCharacterEncoding() { return null; }
			
			@Override
			public Enumeration getAttributeNames() { return null; }
			
			@Override
			public Object getAttribute(String arg0) { return null; }
			
			@Override
			public boolean isUserInRole(String arg0) { return false; }
			
			@Override
			public boolean isRequestedSessionIdValid() { return false; }
			
			@Override
			public boolean isRequestedSessionIdFromUrl() { return false; }
			
			@Override
			public boolean isRequestedSessionIdFromURL() { return false; }
			
			@Override
			public boolean isRequestedSessionIdFromCookie() { return false; }
			
			@Override
			public Principal getUserPrincipal() { return null; }
			
			@Override
			public HttpSession getSession(boolean arg0) { return null; }
			
			@Override
			public HttpSession getSession() { return null; }
			
			@Override
			public String getServletPath() { return null; }
			
			@Override
			public String getRequestedSessionId() { return null; }
			
			@Override
			public StringBuffer getRequestURL() { return null; }
			
			@Override
			public String getRequestURI() { return null; }
			
			@Override
			public String getRemoteUser() { return null; }
			
			@Override
			public String getQueryString() { return null; }
			
			@Override
			public String getPathTranslated() { return null; }
			
			@Override
			public String getPathInfo() { return null; }
			
			@Override
			public String getMethod() { return null; }
			
			@Override
			public int getIntHeader(String arg0) { return 0; }
			
			@Override
			public Enumeration getHeaders(String arg0) { return null; }
			
			@Override
			public Enumeration getHeaderNames() { return null; }
			
			@Override
			public String getHeader(String arg0) { return null; }
			
			@Override
			public long getDateHeader(String arg0) { return 0; }
			
			@Override
			public Cookie[] getCookies() { return null; }
			
			@Override
			public String getContextPath() { return null; }
			
			@Override
			public String getAuthType() { return null; }
		});
		ctx.makeCurrent();
		ctx.setAutentication(AuthenticationInfo.getCurrent());

		return ctx;
	}
	
}
