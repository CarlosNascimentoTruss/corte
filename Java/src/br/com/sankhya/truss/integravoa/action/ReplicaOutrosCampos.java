package br.com.sankhya.truss.integravoa.action;
	
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.truss.integravoa.helper.IntegraVoaHelper;
	
public class ReplicaOutrosCampos implements AcaoRotinaJava {
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		
		IntegraVoaHelper.replicaCampos(ctx);

	}
}