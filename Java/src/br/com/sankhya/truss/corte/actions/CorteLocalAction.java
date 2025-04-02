package br.com.sankhya.truss.corte.actions;

import java.math.BigDecimal;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.truss.corte.scheduled.CorteLocal;

public class CorteLocalAction implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		
		try {
			Registro[] linhas = ctx.getLinhas();
			
			for(Registro linha : linhas) {
				BigDecimal nunota = (BigDecimal) linha.getCampo("NUNOTA");
				
				CorteLocal corte = new CorteLocal();
				
				corte.executaCorteLocal(nunota);
				
			}
		} catch(Exception e) {
			e.printStackTrace();
			ctx.mostraErro("Erro ao executar ação do corte local: \n" + e.getMessage());
		}
		
	}

}
