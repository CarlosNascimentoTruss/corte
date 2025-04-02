package br.com.sankhya.truss.atualpedlote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.Principal;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.sankhya.util.XMLUtils;

import org.jdom.Element;
import br.com.sankhya.dwf.services.ServiceUtils;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.mgecomercial.model.centrais.cac.CACSP;
import br.com.sankhya.mgecomercial.model.centrais.cac.CACSPHome;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class AtualPedLote implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		
		JapeSession.SessionHandle hnd = null;
		JdbcWrapper jdbc = null;
		
		JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
		JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
		
		try {
			hnd = JapeSession.open();
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbc = dwfEntityFacade.getJdbcWrapper();
			
			
			Registro[] linhas = ctx.getLinhas();
			
			
			
			for(Registro linha : linhas) {
				
				BigDecimal nunota = (BigDecimal) linha.getCampo("NUNOTA");
				
				cabDAO.prepareToUpdateByPK(nunota)
				.set("AD_DESCONSCORTE", "S")
				.update();
				
				String query = "SELECT T.* FROM\r\n"
						+ "(\r\n"
						+ "SELECT\r\n"
						+ "ITE.NUNOTA, ITE.CODPROD, CONF.CONTROLE, ITE.VLRUNIT, ITE.VLRTOT, ITE.CODLOCALORIG, ITE.CODVOL, ITE.VLRDESC, ITE.PERCDESC,  SUM(NVL(CONF.QTDUNITCONF, ITE.QTDNEG)) AS QTDNEG\r\n"
						+ "\r\n"
						+ "FROM (SELECT\r\n"
						+ "ITE.NUNOTA,\r\n"
						+ "ITE.CODPROD,\r\n"
						+ "SUM(ITE.QTDNEG) AS QTDNEG,\r\n"
						+ "ITE.CONTROLE,\r\n"
						+ "ITE.VLRUNIT,\r\n"
						+ "SUM(ITE.QTDNEG) * ITE.VLRUNIT AS VLRTOT,\r\n"
						+ "ITE.CODLOCALORIG,\r\n"
						+ "ITE.CODVOL,\r\n"
						+ "ITE.PERCDESC,\r\n"
						+ "ITE.VLRDESC\r\n"
						+ "FROM TGFITE ITE\r\n"
						+ "GROUP BY \r\n"
						+ "ITE.NUNOTA,\r\n"
						+ "ITE.CODPROD,\r\n"
						+ "ITE.CONTROLE,\r\n"
						+ "ITE.VLRUNIT,\r\n"
						+ "ITE.CODLOCALORIG,\r\n"
						+ "ITE.CODVOL,\r\n"
						+ "ITE.PERCDESC,\r\n"
						+ "ITE.VLRDESC) ITE\r\n"
						+ "LEFT JOIN AD_TRASITEMCONF CONF ON CONF.NUNOTA = ITE.NUNOTA AND CONF.CODPROD = ITE.CODPROD \r\n"
						+ "\r\n"
						+ "GROUP BY ITE.NUNOTA, ITE.CODPROD, CONF.CONTROLE, ITE.VLRUNIT, ITE.VLRTOT, ITE.CODLOCALORIG, ITE.CODVOL, ITE.VLRDESC, ITE.PERCDESC\r\n"
						+ "\r\n"
						+ "UNION ALL\r\n"
						+ "\r\n"
						+ "SELECT\r\n"
						+ "\r\n"
						+ "ITE.NUNOTA, ITE.CODPROD, ITE.CONTROLE, ITE.VLRUNIT, ITE.VLRTOT, ITE.CODLOCALORIG, ITE.CODVOL, ITE.VLRDESC, ITE.PERCDESC, SUM((ITE.QTDNEG - CONF.QTDUNITCONF)) AS QTDNEG\r\n"
						+ "\r\n"
						+ "FROM (SELECT\r\n"
						+ "ITE.NUNOTA,\r\n"
						+ "ITE.CODPROD,\r\n"
						+ "SUM(ITE.QTDNEG) AS QTDNEG,\r\n"
						+ "ITE.CONTROLE,\r\n"
						+ "ITE.VLRUNIT,\r\n"
						+ "SUM(ITE.QTDNEG) * ITE.VLRUNIT AS VLRTOT,\r\n"
						+ "ITE.CODLOCALORIG,\r\n"
						+ "ITE.CODVOL,\r\n"
						+ "ITE.PERCDESC,\r\n"
						+ "ITE.VLRDESC\r\n"
						+ "FROM TGFITE ITE\r\n"
						+ "GROUP BY \r\n"
						+ "ITE.NUNOTA,\r\n"
						+ "ITE.CODPROD,\r\n"
						+ "ITE.CONTROLE,\r\n"
						+ "ITE.VLRUNIT,\r\n"
						+ "ITE.CODLOCALORIG,\r\n"
						+ "ITE.CODVOL,\r\n"
						+ "ITE.PERCDESC,\r\n"
						+ "ITE.VLRDESC) ITE\r\n"
						+ "JOIN AD_TRASITEMCONF CONF ON CONF.NUNOTA = ITE.NUNOTA AND CONF.CODPROD = ITE.CODPROD\r\n"
						+ "\r\n"
						+ "AND ((SELECT SUM(C.QTDUNITCONF) FROM AD_TRASITEMCONF C WHERE C.NUNOTA = ITE.NUNOTA AND C.CODPROD = ITE.CODPROD) <> ITE.QTDNEG)\r\n"
						+ "\r\n"
						+ "GROUP BY ITE.NUNOTA, ITE.CODPROD, ITE.CONTROLE, ITE.VLRUNIT, ITE.VLRTOT, ITE.CODLOCALORIG, ITE.CODVOL, ITE.VLRDESC, ITE.PERCDESC\r\n"
						+ ") T\r\n"
						+ "WHERE T.NUNOTA = :P_NUNOTA";
				
				NativeSql q = new NativeSql(jdbc);
				q.setNamedParameter("P_NUNOTA", nunota);
				ResultSet r = q.executeQuery(query);
				
				iteDAO.deleteByCriteria("NUNOTA = ?", nunota);
				
				while(r.next()) {
					this.incluirItem(
							nunota, 
							r.getBigDecimal("CODPROD"), 
							r.getString("CONTROLE"), 
							r.getBigDecimal("VLRUNIT"), 
							r.getBigDecimal("VLRTOT"), 
							r.getBigDecimal("CODLOCALORIG"), 
							r.getString("CODVOL"), 
							r.getBigDecimal("VLRDESC"), 
							r.getBigDecimal("PERCDESC"),
							r.getBigDecimal("QTDNEG"));
				}
				
				cabDAO.prepareToUpdateByPK(nunota)
				.set("AD_DESCONSCORTE", "N")
				.update();
				
				ctx.setMensagemRetorno("Concluído");
				
				
			}
			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			ctx.mostraErro(e.getMessage());
		} finally {
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
		}
		
		
	}
	
	public static void incluirItem(
				                   BigDecimal nunota,
								   BigDecimal codprod,
								   String controle,
								   BigDecimal vlrunit,
								   BigDecimal vlrtot,
								   BigDecimal codlocalorig,
								   String codvol,
								   BigDecimal vlrdesc,
								   BigDecimal percdesc,
								   BigDecimal qtdneg) throws Exception {
		CACSP cacSP = (CACSP) ServiceUtils.getStatelessFacade(CACSPHome.JNDI_NAME, CACSPHome.class);
		
		ServiceContext sctx = createServiceContext(); //new ServiceContext(null);
	    //sctx.setAutentication(AuthenticationInfo.getCurrent());
	    //sctx.makeCurrent();
	    
		Element requestBody = sctx.getRequestBody();
		Element responseBody = sctx.getBodyElement();

		responseBody.removeContent();
		requestBody.removeContent();
		
		Element notaElem = new Element("nota");
		XMLUtils.addAttributeElement(notaElem, "NUNOTA", nunota);
		
		String calculardesconto = percdesc.equals(BigDecimal.ZERO) ? "N" : "S";
		String ignorarrecalcdesc = percdesc.equals(BigDecimal.ZERO) ? "S" : "N";
		
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
		XMLUtils.addContentElement(itemElem, "VLRDESC", vlrdesc);
		XMLUtils.addContentElement(itemElem, "PERCDESC", percdesc);
		XMLUtils.addContentElement(itemElem, "QTDNEG", qtdneg);
		XMLUtils.addContentElement(itemElem, "CALCULARDESCONTO", "S");
		XMLUtils.addContentElement(itemElem, "IGNORARRECALCDESC", "N");
		
		
		itensElem.addContent(itemElem);
		notaElem.addContent(itensElem);
		requestBody.addContent(notaElem.detach());
		
		cacSP.incluirAlterarItemNota(sctx);
		
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
			
			public int getRemotePort() { return 0; }
			
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
			
			public int getLocalPort() { return 0; }
			
			public String getLocalName() { return null; }
			
			public String getLocalAddr() { return null; }
			
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
