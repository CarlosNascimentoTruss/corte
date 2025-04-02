package br.com.sankhya.truss.liberacaovencimento;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class LiberaVencEvent implements EventoProgramavelJava {

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

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		liberaVenc(arg0);
	}
	
	private void liberaVenc (PersistenceEvent evt) throws Exception {
		
		DynamicVO newVO = (DynamicVO) evt.getVo();
		DynamicVO oldVO = (DynamicVO) evt.getOldVO();
		JapeWrapper traslibDAO = JapeFactory.dao("AD_TRASLIBVCTOPES");
		JapeWrapper ampDAO = JapeFactory.dao(DynamicEntityNames.APONTAMENTO_MATERIAIS);
		JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
		
		JapeSession.SessionHandle hnd = null;
		JdbcWrapper jdbc = null;
		
		BigDecimal nuapo = null;
		BigDecimal seqapa = null;
		BigDecimal codprodmp = null;
		String controlemp = null;
		BigDecimal idiatv = null;
		
		try {
			hnd = JapeSession.open();
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbc = dwfEntityFacade.getJdbcWrapper();
			
			if (newVO.asTimestamp("DHLIB") != null && oldVO.asTimestamp("DHLIB") == null && newVO.asString("TABELA").equals("TPRIATV") && newVO.asString("REPROVADO").equals("N")) {
				
				DynamicVO traslibVO = traslibDAO.findOne("IDIATV = ? AND SEQLIB = ?", newVO.asBigDecimal("NUCHAVE"), newVO.asBigDecimal("SEQUENCIA"));
				
				if(traslibVO == null) {
					return;
				}
				
				traslibDAO.prepareToUpdate(traslibVO)
				.set("SITUACAO", "L")
				.update();
				
				
				/*
				codprodmp = newVO.asBigDecimal("AD_CODPRODMP");
				controlemp = newVO.asString("AD_CONTROLEMP");
				idiatv = newVO.asBigDecimal("NUCHAVE");
				
				NativeSql q = new NativeSql(jdbc);
				q.setNamedParameter("P_CODPRODMP", codprodmp);
				q.setNamedParameter("P_CONTROLEMP", controlemp);
				q.setNamedParameter("P_IDIATV", idiatv);
				ResultSet r = q.executeQuery("SELECT "
						+ " AMP.* "
						+ " FROM TPRIATV IATV "
						+ " JOIN TPRAPO APO ON APO.IDIATV = IATV.IDIATV "
						+ " JOIN TPRAMP AMP ON AMP.NUAPO = APO.NUAPO "
						+ " WHERE AMP.CODPRODMP = :P_CODPRODMP "
						+ " AND AMP.CONTROLEMP IN (:P_CONTROLEMP, ' ') "
						+ " AND IATV.IDIATV = :P_IDIATV ");
				
				NativeSql q2 = new NativeSql(jdbc);
				q2.setNamedParameter("P_IDIATV", idiatv);
				ResultSet r2 = q2.executeQuery(" SELECT DISTINCT "
						+ " APO.NUAPO, APA.SEQAPA "
						+ " FROM TPRIATV IATV "
						+ " JOIN TPRAPO APO ON APO.IDIATV = IATV.IDIATV "
						+ " JOIN TPRAPA APA ON APA.NUAPO = APO.NUAPO "
						+ " WHERE IATV.IDIATV = :P_IDIATV ");
				
				if(r2.next()) {
					nuapo = r2.getBigDecimal("NUAPO");
					seqapa = r2.getBigDecimal("SEQAPA");
				} else {
					throw new Exception("Não existe apontamento criado");
				}
				
				
				Boolean continua = true;
				String controle = null;
				
				if(r.next()) {
					nuapo = r.getBigDecimal("NUAPO");
					seqapa = r.getBigDecimal("SEQAPA");
					controle = r.getString("CONTROLEMP");
				} else {
					continua = false;
				}
				
				if(continua) {
					DynamicVO ampVO = ampDAO.findByPK(nuapo, seqapa, codprodmp, controle);
					
					BigDecimal qtdapontada = ampVO.asBigDecimal("QTD");
					
					ampDAO.prepareToUpdateByPK(nuapo, seqapa, codprodmp, controle)
					.set("QTD", qtdapontada.add(traslibVO.asBigDecimal("PESO")))
					.set("CONTROLEMP", controlemp)
					.update();
				} else {
					ampDAO.create()
					.set("NUAPO", nuapo)
					.set("SEQAPA", seqapa)
					.set("CODPRODMP", codprodmp)
					.set("CONTROLEMP", controlemp)
					.set("CODVOL", proDAO.findByPK(codprodmp).asString("CODVOL"))
					.set("QTD", traslibVO.asBigDecimal("PESO"))
					.save();
					
					
				}
				*/
				
				
				
			}
			
			if (newVO.asTimestamp("DHLIB") != null && oldVO.asTimestamp("DHLIB") == null && newVO.asString("TABELA").equals("TPRIATV") && newVO.asString("REPROVADO").equals("S")) {
				DynamicVO traslibVO = traslibDAO.findOne("IDIATV = ? AND SEQLIB = ?", newVO.asBigDecimal("NUCHAVE"), newVO.asBigDecimal("SEQUENCIA"));
				
				if(traslibVO == null) {
					return;
				}
				
				traslibDAO.prepareToUpdate(traslibVO)
				.set("SITUACAO", "N")
				.update();
				
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			MGEModelException.throwMe(e);
		} finally {
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
		}
	}
	
	
	
	
}
