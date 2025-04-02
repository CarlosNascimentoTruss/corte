package br.com.sankhya.truss.inventario;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class SubstituiEtiquetaPalete implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
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
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void beforeInsert(PersistenceEvent ctx) throws Exception {
		atualiza(ctx);
	}
	
	private void atualiza(PersistenceEvent ctx) throws Exception {
		// TODO Auto-generated method stub
		DynamicVO newVO = (DynamicVO) ctx.getVo();
		
		JapeWrapper etiquetaDAO = JapeFactory.dao("AD_TRASETIQUETA");
		DynamicVO etiquetaVO = etiquetaDAO.findOne("CODBARRAS = ?", newVO.asString("CODBARRASETIQUETA"));
		
		//String codbarrassubst = etiquetaVO.asString("CODBARRASSUBST");
		
		String codbarrassubst = etiquetaDAO.findOne("CODBARRASSUBST = ?", newVO.asString("CODBARRASETIQUETA")).asString("CODBARRAS");
		
		
		
		if(codbarrassubst != null) {
			JapeWrapper trastreabilidadeDAO = JapeFactory.dao("AD_TRASTREABILIDADE");
			DynamicVO trastreabilidadeVO = trastreabilidadeDAO.findOne(codbarrassubst);
			String etiquetaPalete = trastreabilidadeVO.asString("CODBARRASETIQUETAPALETE");
			
			newVO.setProperty("CODBARRASETIQUETAPALETE", etiquetaPalete);
			
			trastreabilidadeDAO.prepareToUpdate(trastreabilidadeVO)
			.set("CODBARRASETIQUETAPALETE", null)
			.update();
			
			
		}
		
		
		
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
