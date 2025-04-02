package br.com.sankhya.truss.atualrast;

import java.math.BigDecimal;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class AtualRast implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		JapeWrapper rastDAO = JapeFactory.dao("AD_TRASCONF");
		
		
		String nuconf = (String) ctx.getParam("P_NUCONF");
		
		
		rastDAO.prepareToUpdateByPK(new BigDecimal(nuconf))
		.set("STATUS", "A")
		.update();
		
		ctx.setMensagemRetorno("Concluído");
		
		
		
	}

}
