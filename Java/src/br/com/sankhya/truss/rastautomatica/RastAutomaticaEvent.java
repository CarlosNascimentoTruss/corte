package br.com.sankhya.truss.rastautomatica;

import java.math.BigDecimal;
import java.util.Collection;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

public class RastAutomaticaEvent implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent evt) throws Exception {
		// TODO Auto-generated method stub
		atualizaRastreabilidade(evt);
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
	public void beforeUpdate(PersistenceEvent evt) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	
	
	private static void atualizaRastreabilidade(PersistenceEvent evt) throws Exception {
		
		DynamicVO newEtiquetaVO = (DynamicVO) evt.getVo();
		
		BigDecimal idiproc = newEtiquetaVO.asBigDecimalOrZero("NUOP");
		
		JapeWrapper iprocDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_INSTANCIA_PROCESSO);
		JapeWrapper rastDAO = JapeFactory.dao("AD_TRASTREABILIDADE");
		
		String codBarrasNew = newEtiquetaVO.asString("CODBARRAS");
		String tipoprod = newEtiquetaVO.asString("TIPOPROD");
		
		
		
		if(codBarrasNew != null /*&& codBarrasOld == null*/ && !idiproc.equals(BigDecimal.ZERO) && tipoprod.equals("PAL")) {
			
			if(!codBarrasNew.substring(codBarrasNew.length() - 1).equals("P")) {
				return;
			}
			
			DynamicVO iprocVO = iprocDAO.findByPK(idiproc);
			String aplicaRastPalete = iprocVO.asString("AD_RASTPALETE");
			
			
			
			if("S".equals(aplicaRastPalete)) {
				BigDecimal numeroCaixa = newEtiquetaVO.asBigDecimal("NROCAIXA");
				BigDecimal qtdCaixas = newEtiquetaVO.asBigDecimal("QTDCAIXASPAL");
				
				BigDecimal caixaDe = (numeroCaixa.multiply(qtdCaixas)).subtract(qtdCaixas).add(BigDecimal.ONE);
				BigDecimal caixaAte = numeroCaixa.multiply(qtdCaixas);
				
				Collection<DynamicVO> rastsVO = rastDAO.find("IDIPROC = ? AND TO_NUMBER(NROCAIXA) BETWEEN ? AND ?", idiproc, caixaDe, caixaAte);
				
				for(DynamicVO rastVO : rastsVO) {
					rastDAO.prepareToUpdateByPK(rastVO.asBigDecimal("CODRAST"))
					.set("CODBARRASETIQUETAPALETE", newEtiquetaVO.asString("CODBARRAS"))
					.update();
				}
			}
		}
	}
	
}

















