package br.com.sankhya.truss.corte.events;

import java.math.BigDecimal;
import java.util.Collection;

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
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truss.corte.helper.CorteHelper;

public class StatusFaturamento implements EventoProgramavelJava {
	private static final long serialVersionUID = 1L;
	private static SessionHandle hnd = null;
	private static JdbcWrapper jdbc	= null;
	private static Boolean isOpen = Boolean.FALSE;
	
	@Override
	public void afterDelete(PersistenceEvent evt) throws Exception {
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
	public void beforeDelete(PersistenceEvent evt) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(PersistenceEvent evt) throws Exception {
		// TODO Auto-generated method stub
		analiseStatusFaturamento(evt, "I");
	}

	@Override
	public void beforeUpdate(PersistenceEvent evt) throws Exception {
		// TODO Auto-generated method stub
		analiseStatusFaturamento(evt, "U");
	}
	
	private void analiseStatusFaturamento(PersistenceEvent evt, String momento) throws Exception {
		
		try {	
			openSession();
			
			JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
			JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
			CorteHelper corteHelper = new CorteHelper();
			
			
			DynamicVO newVO = (DynamicVO) evt.getVo();
			
			BigDecimal nunotaorig = newVO.asBigDecimal("NUNOTAORIG");
			BigDecimal sequenciaorig = newVO.asBigDecimal("SEQUENCIAORIG");
			BigDecimal nunota = newVO.asBigDecimal("NUNOTA");
			
			String corte = corteHelper.validaTopCorte(cabDAO.findByPK(nunotaorig).asBigDecimal("CODTIPOPER"));
			
			String tipmovorig = cabDAO.findByPK(nunotaorig).asString("TIPMOV");
			String tipmovdest = cabDAO.findByPK(nunota).asString("TIPMOV");
			
			if (!("P".equals(tipmovorig) && "V".equals(tipmovdest))) {
				return;
			}
			
			/*
			if(!"S".equals(corte)) {
				return;
			}
			*/
			
			Boolean parcial = false;
			Boolean liberadoParcial = false;
			String status = null;
			
			if("I".equals(momento) || "U".equals(momento)) {
				Collection<DynamicVO> itesVO = iteDAO.find("NUNOTA = ? AND SEQUENCIA <> ?", nunotaorig, sequenciaorig);
				
				for(DynamicVO iteVO : itesVO) {
					if (!iteVO.asBigDecimal("QTDENTREGUE").equals(iteVO.asBigDecimal("QTDNEG"))) {
						parcial = true;
					}
				}
				
				status = parcial ? "24" : "25";
				
				cabDAO.prepareToUpdateByPK(nunotaorig)
				.set("AD_STATUSPED", status)
				.update();
			}
		} catch (Exception e) {
			throw new Exception("Erro no Evento StatusFaturamento: " + e.getMessage());
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
	
}
