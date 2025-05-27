package br.com.sankhya.truss.integraprodutos.action;
	
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.truss.integraprodutos.helper.IntegraProdutosHelper;
	
public class ReplicaOutrosCampos implements AcaoRotinaJava {
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		
		IntegraProdutosHelper.replicaCampos(ctx);

	}
}