package br.com.evolvesolucoes.truss;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class LiberaCalculoRoyalties implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		Registro[] linhas = ctx.getLinhas();
		
		for(Registro linha : linhas) {
			linha.setCampo("AD_ERROROYALTIES", "N");
		}
		
		ctx.setMensagemRetorno("Cálculo de Royalties Liberado");
	}

}
