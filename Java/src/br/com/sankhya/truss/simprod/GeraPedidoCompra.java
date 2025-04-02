package br.com.sankhya.truss.simprod;

import java.math.BigDecimal;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class GeraPedidoCompra implements AcaoRotinaJava {
	private static final long serialVersionUID = 1L;
	private static SessionHandle hnd = null;
	private static JdbcWrapper jdbc	= null;
	private static Boolean isOpen = Boolean.FALSE;
	
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		try {
			openSession();
			 
			JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
			JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
			JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
			JapeWrapper cpaDAO = JapeFactory.dao("AD_CONFPEDCPASIMPROD");
			JapeWrapper simiteDAO = JapeFactory.dao("AD_SIMPRODITE");
			
			DynamicVO cpaVO = cpaDAO.findByPK(BigDecimal.ONE);
			
			DynamicVO cabVO = cabDAO.create()
        			.set("CODPARC", cpaVO.asBigDecimal("CODPARC"))
        			.set("CODEMP", cpaVO.asBigDecimal("CODEMP"))
        			.set("CODNAT", cpaVO.asBigDecimal("CODNAT"))
        			.set("NUMNOTA", BigDecimal.ZERO)
        			.set("CODTIPOPER", cpaVO.asBigDecimal("CODTIPOPER"))
        			.set("CODCENCUS", cpaVO.asBigDecimal("CODCENCUS"))
        			.save();
			
			Registro[] linhas = ctx.getLinhas();

			for(Registro linha : linhas) {
				 
				iteDAO.create()
        		.set("NUNOTA", cabVO.asBigDecimal("NUNOTA"))
        		.set("CODPROD", (BigDecimal) linha.getCampo("CODPROD"))
        		.set("CONTROLE", " ")
        		.set("VLRUNIT", BigDecimal.ZERO)
        		.set("VLRTOT", BigDecimal.ZERO)
        		.set("QTDNEG", (BigDecimal) linha.getCampo("SUGCOMPRA"))
        		.set("CODVOL", (String) linha.getCampo("CODVOL"))
        		.set("CODLOCALORIG", cpaVO.asBigDecimal("CODLOCAL"))
        		.set("ATUALESTOQUE", BigDecimal.ZERO)
        		.save();
				
				simiteDAO.prepareToUpdateByPK((BigDecimal) linha.getCampo("NUSIM"),(BigDecimal) linha.getCampo("CODPROD"))
				.set("NUNOTA", cabVO.asBigDecimal("NUNOTA"))
				.update();
			}
			
			ctx.setMensagemRetorno("Pedido de compra gerado: " + cabVO.asBigDecimal("NUNOTA"));
			
			
		} catch(Exception e) {
			e.printStackTrace();
			MGEModelException.throwMe(new Exception(e.getMessage()));
		} finally {
			closeSession();
		}
	}
	
	private static void openSession() {
		try {
			if (isOpen && jdbc != null) {
				return;
			}
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			hnd = JapeSession.open();
			hnd.setFindersMaxRows(-1);
			hnd.setCanTimeout(false);
			jdbc = dwfFacade.getJdbcWrapper();
			jdbc.openSession();
			isOpen = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void closeSession() {
		if(isOpen) {
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
			isOpen = false;
			jdbc = null;
		}
	}
}
