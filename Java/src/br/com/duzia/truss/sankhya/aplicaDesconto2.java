package br.com.duzia.truss.sankhya;


import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class aplicaDesconto2 implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		
		ctx.mostraErro("ENTROU AQUI");
	}

}
