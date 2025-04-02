package br.com.sankhya.truss.integravoa.scheduled;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import br.com.sankhya.truss.integravoa.helper.IntegraVoaHelper;

public class IntegraVoaScheduled implements ScheduledAction {

	@Override
	public void onTime(ScheduledActionContext ctx) {
		// TODO Auto-generated method stub
		
		try {
			IntegraVoaHelper.integraVoa(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}