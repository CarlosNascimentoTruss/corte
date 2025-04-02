package br.com.sankhya.truss.atualreserva.event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.Principal;
import java.sql.ResultSet;
import java.util.Collection;
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
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.mgecomercial.model.centrais.cac.CACSP;
import br.com.sankhya.mgecomercial.model.centrais.cac.CACSPHome;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class AtualReserva implements EventoProgramavelJava {
	
	String msgerro = "";
	
	@Override
	public void beforeDelete(PersistenceEvent ctx) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent ctx) throws Exception {
		// TODO Auto-generated method stub
		//atualiza(ctx);
		atualiza(ctx);
	}
	
	@Override
	public void beforeInsert(PersistenceEvent ctx) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent ctx) throws Exception {
		// TODO Auto-generated method stub
		atualiza(ctx);
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void afterDelete(PersistenceEvent ctx) throws Exception {
		// TODO Auto-generated method stub
		atualiza(ctx);
	}
	
	
	public void atualiza(PersistenceEvent ctx) throws Exception {
		// TODO Auto-generated method stub
		
		try {
		DynamicVO ampVO = (DynamicVO) ctx.getVo();
		
		JapeWrapper apoDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_APONTAMENTO);
		DynamicVO apoVO = apoDAO.findByPK(ampVO.asBigDecimal("NUAPO"));
		
		//atualReserva(apoVO);
		atualReserva2(apoVO, ampVO.asBigDecimal("CODPRODMP"), ampVO.asString("CONTROLEMP"));
		
		} catch (Exception e){
			e.printStackTrace();
			MGEModelException.throwMe(new Exception(e.getMessage()));
		}
		
	}
	
	private void atualReserva2(DynamicVO apoVO, BigDecimal codprodmp, String controlemp) throws Exception {
		JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
		JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
		JapeWrapper iatvDAO = JapeFactory.dao(DynamicEntityNames.INSTANCIA_ATIVIDADE);
		JapeWrapper apoDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_APONTAMENTO);
		JapeWrapper estDAO = JapeFactory.dao(DynamicEntityNames.ESTOQUE);
		JapeWrapper gruDAO = JapeFactory.dao(DynamicEntityNames.GRUPO_PRODUTO);
		JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
		JapeWrapper topDAO = JapeFactory.dao(DynamicEntityNames.TIPO_OPERACAO);
		
		try {
			msgerro = "1";
			JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
		    
			BigDecimal idiatv = apoVO.asBigDecimal("IDIATV");
			
			BigDecimal idiproc = iatvDAO.findByPK(idiatv).asBigDecimal("IDIPROC");
			msgerro = "2";
			Collection<DynamicVO> cabVOs = cabDAO.find("IDIPROC = ? AND TIPMOV = 'J'", idiproc);
			
			if(cabVOs.size() == 0) {
				//throw new Exception("Ordem de produção não possui nota de reserva., Verifique.");
				return;
			}
			
			if(cabVOs.size() > 1) {
				//throw new Exception("Ordem de produção possui mais de uma nota de reserva. Verifique.");
				return;
			}
			msgerro = "3";
			DynamicVO cabVO = cabVOs.iterator().next();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.loadSql(this.getClass(), "atualizaRequisicao.sql");
			sql.setNamedParameter("P_NUNOTA", cabVO.asBigDecimal("NUNOTA"));
			sql.setNamedParameter("P_NUAPO", apoVO.asBigDecimal("NUAPO"));
			msgerro = "4";
			ResultSet r = sql.executeQuery();
			msgerro = "4.1";
			cabDAO.prepareToUpdateByPK(cabVO.asBigDecimal("NUNOTA"))
			.set("AD_DESCONSCORTE", "S")
			.update();
			msgerro = "4.2";
			iteDAO.deleteByCriteria("NUNOTA = ? AND CODPROD = ?", cabVO.asBigDecimal("NUNOTA"), codprodmp);
			
			msgerro = "5" + " - " + cabVO.asBigDecimal("NUNOTA") + " - " + codprodmp;
			
			int count = 0;
			while(r.next()) {	
				count++;
				DynamicVO estVO = null;
				msgerro = "5.1";
				if(r.getBigDecimal("CODPRODMP").equals(codprodmp)) {
					msgerro = "5.2";
					if(r.getString("CONTROLEMP").equals(" ")) {
						estVO = estDAO.findOne("CODPROD = ? AND CONTROLE = ? AND CODLOCAL = ? AND CODPARC = 0 AND CODEMP = 6 AND AD_ATIVOLOTE = 'S'",
								r.getBigDecimal("CODPRODMP"),
								r.getString("CONTROLEMP"),
								r.getBigDecimal("CODLOCALBAIXA")
							);
						msgerro = "5.3";
					} else {
						estVO = estDAO.findOne("CODPROD = ? AND CODLOCAL = ? AND CODPARC = 0 AND CODEMP = 6 AND AD_ATIVOLOTE = 'S'",
								r.getBigDecimal("CODPRODMP"),
								r.getBigDecimal("CODLOCALBAIXA")
							);
						msgerro = "5.4";
					}
					msgerro = "5.5";
					//BigDecimal estoqueOld = estVO != null ? estVO.asBigDecimal("ESTOQUE").add(BigDecimal.valueOf(900000000)) : BigDecimal.valueOf(900000000);
					msgerro = "6";
					/*estVO.setProperty("ESTOQUE", BigDecimal.valueOf(900000000));
					estDAO.prepareToUpdate(estVO)
					.update();*/
					
					
								//realiza operações em entidades ou NativeSQL
								incluirItem2(
										cabVO.asBigDecimal("NUNOTA"), 
										r.getBigDecimal("CODPRODMP"), 
										r.getString("CONTROLEMP"), 
										r.getBigDecimal("VLRUNIT"), 
										r.getBigDecimal("VLRTOT"),
										r.getBigDecimal("CODLOCALBAIXA"), 
										r.getString("CODVOL"), 
										r.getBigDecimal("QTD"),
										count);
								
								/*estVO.setProperty("ESTOQUE", estoqueOld);
								estDAO.prepareToUpdate(estVO)
								.update();*/
								
								msgerro = "7";	
				}
				
			}
				
			cabDAO.prepareToUpdateByPK(cabVO.asBigDecimal("NUNOTA"))
			.set("AD_DESCONSCORTE", "N")
			.update();
			msgerro = "8";
		} catch (Exception e){
			e.printStackTrace();
			throw new Exception(e.getMessage() + "/ " + msgerro);
		}
		
	}
	
	
	
	private static void incluirItem2(
            BigDecimal nunota,
			   BigDecimal codprod,
			   String controle,
			   BigDecimal vlrunit,
			   BigDecimal vlrtot,
			   BigDecimal codlocalorig,
			   String codvol,
			   
			   BigDecimal qtdneg,
			   int count) throws Exception 
	{
		try {
		JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
		
			iteDAO.create()
			.set("NUNOTA", nunota)
			.set("CODPROD", codprod)
			.set("CONTROLE", controle)
			.set("VLRUNIT", vlrunit)
			.set("VLRTOT", vlrtot)
			.set("CODLOCALORIG", codlocalorig)
			.set("CODVOL", codvol)
			.set("VLRDESC", BigDecimal.ZERO)
			.set("PERCDESC", BigDecimal.ZERO)
			.set("QTDNEG", qtdneg)
			.set("RESERVA", "S")
			.set("ATUALESTOQUE", BigDecimal.ONE)
			.save();
		} catch (Exception e){
			
			e.printStackTrace();
			throw new Exception(e.getMessage() + " - " + nunota + " - " + codprod + " - " + controle + " - " + qtdneg);
			}
		
	}
	
	private static void incluirItem(
            BigDecimal nunota,
			   BigDecimal codprod,
			   String controle,
			   BigDecimal vlrunit,
			   BigDecimal vlrtot,
			   BigDecimal codlocalorig,
			   String codvol,
			   
			   BigDecimal qtdneg,
			   int count) throws Exception 
		{
			CACSP cacSP = (CACSP) ServiceUtils.getStatelessFacade(CACSPHome.JNDI_NAME, CACSPHome.class);
			
			ServiceContext sctx = createServiceContext();
			
			Element requestBody = sctx.getRequestBody();
			Element responseBody = sctx.getBodyElement();
			try {
			
			
			responseBody.removeContent();
			requestBody.removeContent();
			
			Element notaElem = new Element("nota");
			XMLUtils.addAttributeElement(notaElem, "NUNOTA", nunota);
			
			Element itensElem = new Element("itens");
			Element itemElem = new Element("item");
			XMLUtils.addContentElement(itemElem, "NUNOTA", nunota);
			XMLUtils.addContentElement(itemElem, "CODPROD", codprod);
			XMLUtils.addContentElement(itemElem, "CONTROLE", controle);
			XMLUtils.addContentElement(itemElem, "SEQUENCIA", "");
			XMLUtils.addContentElement(itemElem, "VLRUNIT", vlrunit);
			XMLUtils.addContentElement(itemElem, "VLRTOT", vlrtot);
			XMLUtils.addContentElement(itemElem, "CODLOCALORIG", codlocalorig);
			XMLUtils.addContentElement(itemElem, "CODVOL", codvol);
			XMLUtils.addContentElement(itemElem, "VLRDESC", BigDecimal.ZERO);
			XMLUtils.addContentElement(itemElem, "PERCDESC", BigDecimal.ZERO);
			XMLUtils.addContentElement(itemElem, "QTDNEG", qtdneg);
			//XMLUtils.addContentElement(itemElem, "CALCULARDESCONTO", "S");
			//XMLUtils.addContentElement(itemElem, "IGNORARRECALCDESC", "N");
			
			itensElem.addContent(itemElem);
			notaElem.addContent(itensElem);
			requestBody.addContent(notaElem.detach());
			
			
			
			
			cacSP.incluirAlterarItemNota(sctx);
			} catch (Exception e){
			
			e.printStackTrace();
			throw new Exception(e.getMessage() + " - " + nunota + " - " + codprod + " - " + controle + " - " + qtdneg);
			}
		}
		
	
		@Override
		public void beforeUpdate(PersistenceEvent arg0) throws Exception {
			// TODO Auto-generated method stub
			
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
