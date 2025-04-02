package br.com.sankhya.truss.integraprodutos.scheduled;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import br.com.sankhya.truss.integraprodutos.helper.IntegraProdutosHelper;

public class IntegraProdutosScheduled implements ScheduledAction {

	@Override
	public void onTime(ScheduledActionContext ctx) {
		// TODO Auto-generated method stub
		
		try {
			IntegraProdutosHelper.integraProdutos(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}