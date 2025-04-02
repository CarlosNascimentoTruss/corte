package br.com.sankhya.truss.lysi.truss.eventos;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import br.com.sankhya.truss.lysi.truss.geral.GeradorProducao;


public class EventoGerarProducao  implements ScheduledAction {
	
	
	public void onTime(ScheduledActionContext contexto) {
		
		try {
			new GeradorProducao().gerarProducao();
		} catch (Exception e) {
			System.out.println("*** GERADOR PRODUCAO - ERRO ***");
			e.printStackTrace();
		} 
		
	}
	
	
	
}
