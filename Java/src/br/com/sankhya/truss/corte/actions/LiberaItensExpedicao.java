package br.com.sankhya.truss.corte.actions;

import java.math.BigDecimal;
import java.sql.ResultSet;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class LiberaItensExpedicao implements AcaoRotinaJava {
	private static SessionHandle hnd = null;
	private static JdbcWrapper jdbc	= null;
	private static Boolean isOpen = Boolean.FALSE;
	private static String mensagemErro = "";
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		try {
			openSession();
			JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
			JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
			JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
			Registro[] linhas = ctx.getLinhas();
			
			String liberaIntegral = (String) ctx.getParam("P_LIBERAINTEGRAL");
			BigDecimal qtdLiberar = (Double) ctx.getParam("P_QTDLIBERAR") == null ? BigDecimal.ZERO : new BigDecimal((Double) ctx.getParam("P_QTDLIBERAR"));
			
			if("S".equals(liberaIntegral)) {
				BigDecimal nunota = (BigDecimal) linhas[0].getCampo("NUNOTA");
				liberaIntegral(nunota);
				
				cabDAO.prepareToUpdateByPK(nunota)
				.set("AD_DTLIBEXP", TimeUtils.getNow())
				.update();
				
				ctx.setMensagemRetorno(mensagemErro.equals("") ?  "Itens Liberados." : mensagemErro);
				return;
			}
			
			if(!"S".equals(liberaIntegral) && qtdLiberar.equals(BigDecimal.ZERO)) {
				ctx.mostraErro("Se não liberar o pedido integralmente, um valor de itens liberados deve ser informado.");
			}
			
			BigDecimal nunota = null;
			
			for(Registro linha : linhas) {
			
				
				nunota = (BigDecimal) linha.getCampo("NUNOTA");
				
				BigDecimal qtdConferida = (BigDecimal) linha.getCampo("QTDCONFERIDA");
				BigDecimal codprod = (BigDecimal) linha.getCampo("CODPROD");
				BigDecimal codemp = (BigDecimal) linha.getCampo("CODEMP");
				BigDecimal qtdneg = (BigDecimal) linha.getCampo("QTDNEG");
				BigDecimal codlocalorig = (BigDecimal) linha.getCampo("CODLOCALORIG");
				BigDecimal disponivel = BigDecimal.ZERO;
				
				BigDecimal qtdMinVenda = proDAO.findByPK(codprod).asBigDecimalOrZero("AD_QTDMINVENDA");
				
				if(qtdMinVenda.compareTo(BigDecimal.ZERO) <= 0) {
					ctx.mostraErro("Produto sem quantidade mínima de venda cadastrado.");
				}
				
				if(!isMultiplo(qtdLiberar, qtdMinVenda)) {
					ctx.mostraErro("Indique uma quantidade múltiplo da qtd. mínima de venda do produto: " + qtdMinVenda);
				}
				
				if(qtdLiberar.compareTo(qtdConferida) > 0) {
					ctx.mostraErro("Quantidade informada maior que a quantidade cortada.");
				}
				
				NativeSql queryEstLocal = new NativeSql(jdbc);
				queryEstLocal.setNamedParameter("P_CODPROD", codprod);
				queryEstLocal.setNamedParameter("P_CODEMP", codemp);
				queryEstLocal.setNamedParameter("P_CODLOCAL", codlocalorig);
				
				ResultSet rsEstLocal = queryEstLocal.executeQuery("SELECT SUM(ESTOQUE) AS DISPONIVEL"
						+ " FROM TGFEST "
						+ " WHERE CODPROD = :P_CODPROD "
						+ " AND CODPARC = 0 "
						+ " AND CODEMP = :P_CODEMP "
						+ " AND CODLOCAL = :P_CODLOCAL "
						+ " AND AD_ATIVOLOTE = 'S' ");
				
				
				if(rsEstLocal.next()) {
					disponivel = rsEstLocal.getBigDecimal("DISPONIVEL");
				}
				
				
				
				if(qtdLiberar.compareTo(disponivel) > 0) {
					ctx.mostraErro("Não existe quantidade disponível suficiente para liberar a quantidade solicitada. \n Quantidade Disponível: " + disponivel);
				}
				
				linha.setCampo("QTDCONFERIDA", qtdConferida.subtract(qtdLiberar));
				
				cabDAO.prepareToUpdateByPK(nunota)
				.set("AD_DTLIBEXP", TimeUtils.getNow())
				.update();
				
			}
			
			String status = "20";
			
			for(DynamicVO iteVO : iteDAO.find("NUNOTA = ?", nunota)) {
				if (iteVO.asBigDecimal("QTDCONFERIDA").compareTo(BigDecimal.ZERO) > 0) {
					status = "19";
				}
			}
			
			cabDAO.prepareToUpdateByPK(nunota)
			.set("AD_STATUSPED", status)
			.update();
			
			ctx.setMensagemRetorno(mensagemErro.equals("") ?  "Item liberado com sucesso." : mensagemErro);
			
		} catch (Exception e) {
			e.printStackTrace();
			ctx.mostraErro(e.getMessage());
		} finally {
			closeSession();
		}
	}
	
	private void liberaIntegral(BigDecimal nunota) throws Exception {
		
		JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
		JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
		
		int count = 0;
		String continua = "";
		for(DynamicVO iteVO : iteDAO.find("NUNOTA = ? AND QTDCONFERIDA > 0", nunota)) {
			continua = verificaEstoque(iteVO.asBigDecimal("CODPROD"), 
							iteVO.asBigDecimal("CODEMP"), 
							iteVO.asBigDecimal("CODLOCALORIG"), 
							iteVO.asBigDecimal("QTDCONFERIDA"), 
							iteVO.asBigDecimal("QTDNEG"));
			if(continua.equals("S")) {
				iteDAO.prepareToUpdate(iteVO)
				.set("QTDCONFERIDA", BigDecimal.ZERO)
				.update();
			} else {
				count++;
				mensagemErro = "Itens Liberados. Alguns itens não puderam ser liberados por falta de estoque. Verifique o campo Qtd. Corte na aba de itens.";
			}
		}
		
		if("N".equals(continua)) {
			cabDAO.prepareToUpdateByPK(nunota)
			.set("AD_STATUSPED", "19")
			.update();
		} else {
			cabDAO.prepareToUpdateByPK(nunota)
			.set("AD_STATUSPED", "20")
			.update();
		}
		
	}
	
	private String verificaEstoque(BigDecimal codprod, BigDecimal codemp, BigDecimal codlocalorig, BigDecimal qtdLiberar, BigDecimal qtdneg) throws Exception {
		
		NativeSql queryEstLocal = new NativeSql(jdbc);
		queryEstLocal.setNamedParameter("P_CODPROD", codprod);
		queryEstLocal.setNamedParameter("P_CODEMP", codemp);
		queryEstLocal.setNamedParameter("P_CODLOCAL", codlocalorig);
		
		BigDecimal disponivel = BigDecimal.ZERO;
		
		ResultSet rsEstLocal = queryEstLocal.executeQuery("SELECT SUM(TOTALESTOQUE - RESERVADOPEDIDOS) AS DISPONIVEL "
				+ " FROM AD_VW_QTDLIBEXPEDICAO "
				+ " WHERE CODPROD = :P_CODPROD "
				+ " AND CODLOCAL = :P_CODLOCAL "
				+ " AND CODEMP = :P_CODEMP ");
		
		
		if(rsEstLocal.next()) {
			disponivel = rsEstLocal.getBigDecimal("DISPONIVEL");//.add(qtdneg);//.subtract(estoqueTerceiros);
		}
		
		if(qtdLiberar.compareTo(disponivel) > 0) {
			return "N";
		}
		return "S";
	}
	
	public static boolean isMultiplo(BigDecimal valor, BigDecimal multiplo) {
        
        BigDecimal resto = valor.remainder(multiplo);
        
        // Um número é múltiplo se o resto da divisão for zero
        return resto.compareTo(BigDecimal.ZERO) == 0;
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
