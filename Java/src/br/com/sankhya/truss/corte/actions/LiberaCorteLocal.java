package br.com.sankhya.truss.corte.actions;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class LiberaCorteLocal implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		try {
		Registro[] linhas = ctx.getLinhas();
		
		for(Registro linha : linhas) {
			linha.setCampo("AD_BLOQCORTELOCAL", "N");
		}
		
		ctx.setMensagemRetorno("Pedido(s) liberado(s) para corte local. Aguarde a execução da ação agendada.");
		
		} catch (Exception e) {
			e.printStackTrace();
			ctx.mostraErro(e.getMessage());
		}
	}

}
