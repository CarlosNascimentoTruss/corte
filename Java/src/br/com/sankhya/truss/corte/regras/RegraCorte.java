package br.com.sankhya.truss.corte.regras;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.vo.PrePersistEntityState;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.ContextoRegra;
import br.com.sankhya.modelcore.comercial.Regra;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truss.corte.helper.CorteHelper;


public class RegraCorte implements Regra {
	private static final long serialVersionUID = 1L;
	private static SessionHandle hnd = null;
	private static JdbcWrapper jdbc	= null;
	private static Boolean isOpen = Boolean.FALSE;
	
	public void afterUpdate(ContextoRegra arg0) throws Exception {
		
		PrePersistEntityState state = arg0.getPrePersistEntityState();
		DynamicVO newVO = state.getNewVO();
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		CorteHelper corteHelper = new CorteHelper();
		JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
		
		try {
			openSession();
			
			// AO CONFIRMAR UM ORÇAMENTO QUE TEM NA TOP A MARCAÇÃO "Passa pela Rotina de Corte", O STATUS DO PEDIDO PASSA A SER 18 - Enviado para Aprovação
			if (isCabecalho(newVO)) {
				String statusNota = arg0.getPrePersistEntityState().getNewVO().asString("STATUSNOTA");
				String statusNotaOld = arg0.getPrePersistEntityState().getOldVO().asString("STATUSNOTA");
				
				if(!"1".equals(newVO.asString("AD_STATUSPED"))) {
					return;
				}
				
				if (statusNota.equals("L")) {
					
					
					String corte = corteHelper.validaTopCorte(newVO.asBigDecimal("CODTIPOPER"));
					if("N".equals(corte)) {
						return;
					}
					newVO.setProperty("AD_STATUSPED", "18");
					dwfEntityFacade.saveEntity(DynamicEntityNames.CABECALHO_NOTA, (EntityVO) newVO);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			throw new Exception("Falha na regra de negócio Java \n" + e.getMessage());
		} finally {
			closeSession();
		}
	}
	
	private boolean isCabecalho(DynamicVO vo) {
		return vo.getValueObjectID().indexOf(DynamicEntityNames.CABECALHO_NOTA) > -1;
	}
	
	private boolean isItem(DynamicVO vo) {
		return vo.getValueObjectID().indexOf(DynamicEntityNames.ITEM_NOTA) > -1;
	}
	
	private boolean isFinanceiro(DynamicVO vo) {
		return vo.getValueObjectID().indexOf(DynamicEntityNames.FINANCEIRO) > -1;
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
	public void afterDelete(ContextoRegra arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(ContextoRegra arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(ContextoRegra arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(ContextoRegra arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(ContextoRegra arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
