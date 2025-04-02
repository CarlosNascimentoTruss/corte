package br.com.sankhya.truss.corte.events;



import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.AtributosRegras;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truss.corte.helper.CorteHelper;
import br.com.sankhya.ws.ServiceContext;

public class ValidaPermissaoUsuario implements EventoProgramavelJava {
	private static final long serialVersionUID = 1L;
	private static SessionHandle hnd = null;
	private static JdbcWrapper jdbc	= null;
	private static Boolean isOpen = Boolean.FALSE;
	
	@Override
	public void beforeUpdate(PersistenceEvent evt) throws MGEModelException {
		// TODO Auto-generated method stub
		
		JapeWrapper gruDAO = JapeFactory.dao(DynamicEntityNames.GRUPO_USUARIO);
		JapeWrapper usuDAO = JapeFactory.dao(DynamicEntityNames.USUARIO);
		CorteHelper corteHelper = new CorteHelper();
		
		try {
			openSession();
			DynamicVO cabVO = (DynamicVO) evt.getVo();
			
			boolean confirmando = JapeSession.getProperty(AtributosRegras.CONFIRMANDO) != null;
			
			String corte = corteHelper.validaTopCorte(cabVO.asBigDecimal("CODTIPOPER"), jdbc);
			if("N".equals(corte) || confirmando) {
				return;
			}
			
			BigDecimal usuarioLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
			String permiteAlterar = gruDAO.findByPK(usuDAO.findByPK(usuarioLogado).asBigDecimal("CODGRUPO")).asString("AD_ALTERAPEDCONF");
			
			if("L".equals(cabVO.asString("STATUSNOTA"))  && !"S".equals(permiteAlterar)) {
				throw new Exception("Usuário não tem permissão para alterar documento confirmado. Verifique as permissões no grupo de usuários.");
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			MGEModelException.throwMe(new Exception("Falha no evento ValidaPermissaoUsuario " + e.getMessage()));
		} finally {
			closeSession();
		}
		
	}
	
	private static void openSession() {
		try {
			if (isOpen && jdbc != null) {
				return;
			}
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			hnd = JapeSession.open();
			hnd.setFindersMaxRows(-1);
			hnd.setCanTimeout(false);
			jdbc = dwfFacade.getJdbcWrapper();
			jdbc.openSession();
			isOpen = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void closeSession() {
		if(isOpen) {
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
			isOpen = false;
			jdbc = null;
		}
	}
	
	
	
	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	
}
