package br.com.sankhya.truss.integravoa.action;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.truss.integravoa.helper.IntegraVoaHelper;

public class IntegraVoaAction implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		try {
			
			
			
			IntegraVoaHelper.integraVoa(null);
		} catch(Exception e) {
			ctx.mostraErro(e.getMessage());
		}
		
	}

}
