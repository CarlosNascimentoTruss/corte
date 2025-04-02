package br.com.sankhya.truss.lysi.truss.botoes;

import br.com.sankhya.truss.lysi.truss.geral.GeradorProducao;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class BotaoGerarProducao implements AcaoRotinaJava{

	@Override
	public void doAction(final ContextoAcao contexto) throws Exception {
		
		new GeradorProducao().gerarProducao();
		
	}
	
	

}
