package br.com.sankhya.truss.verba;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.CentralFinanceiro;
import br.com.sankhya.modelcore.comercial.CentralItemNota;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class AplicaVerbaAction implements AcaoRotinaJava {
	
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		try {
			Registro[] linhas = ctx.getLinhas();
			
			for (Registro linha : linhas) {
				aplicaVerba((BigDecimal) linha.getCampo("NUNOTA"));
			}
		} catch(Exception e) {
			ctx.mostraErro(e.getMessage());
		}
		ctx.setMensagemRetorno("Verba calculada com sucesso");
		
	}
	
	
	private static void aplicaVerba(BigDecimal nunota) throws Exception {
		Map<?, ?> propriedades = JapeSession.getProperties();
		for (Map.Entry<?, ?> entry : propriedades.entrySet()) {
		    System.out.println("[PROPRIEDADE]: " + entry.getKey() + " = " + entry.getValue());
		}
		
		
		
		//DynamicVO newVO = (DynamicVO) ctx.getVo();
		JapeWrapper verbaDAO = JapeFactory.dao("AD_CONTROLEVERBAS");
		JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
		JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
		JapeWrapper prefDAO = JapeFactory.dao(DynamicEntityNames.PARAMETRO_SISTEMA);
		JapeWrapper varDAO = JapeFactory.dao(DynamicEntityNames.COMPRA_VENDA_VARIOS_PEDIDO);
		
		//BigDecimal nunota = newVO.asBigDecimal("NUNOTA");
		
		DynamicVO cabVO = cabDAO.findByPK(nunota);
		
		// Faz a restauração da verba antes de fazer um novo cálculo. Ação necessária por ser executada em cada item.
		limpaHistoricoVerba(nunota);
		
		if(cabVO == null) {
			return;
		}
		
		if(!cabVO.asString("TIPMOV").equals("V")) {
			return;
		}
		
		BigDecimal codparc = cabVO.asBigDecimal("CODPARC");
		Timestamp dtneg = cabVO.asTimestamp("DTNEG");
		BigDecimal vlrDisponivelVerba = BigDecimal.ZERO;
		BigDecimal vlrTotItens = BigDecimal.ZERO;
		BigDecimal vlrTotItensSemIPI = BigDecimal.ZERO;
		BigDecimal percMax = prefDAO.findOne("CHAVE = ?", "PERCMAXVERBA").asBigDecimal("NUMDEC");
		
		Collection<DynamicVO> itesVO = iteDAO.findAndOrderBy("NUNOTA = ?", "SEQUENCIA", nunota);
		
		for(DynamicVO iteVO : itesVO) {
			BigDecimal vlrIPI = iteVO.asBigDecimal("ALIQIPI").multiply(iteVO.asBigDecimal("VLRTOT")).divide(BigDecimal.valueOf(100), 5, RoundingMode.HALF_UP);
			vlrTotItens = vlrTotItens.add(iteVO.asBigDecimal("VLRTOT").add(vlrIPI));
			vlrTotItensSemIPI = vlrTotItensSemIPI.add(iteVO.asBigDecimal("VLRTOT"));
		}
		
		if(vlrTotItens.compareTo(BigDecimal.ZERO) <= 0) {
			cabDAO.prepareToUpdate(cabVO)
			.set("AD_VALORVERBA", BigDecimal.ZERO)
			.set("AD_VERBAAPLICADA", BigDecimal.ONE)
			.update();
			return;
		}
		
		if(percMax == null) {
			throw new Exception("Não foi possível calcular a verba. Informe um valor para o parâmetro Percentual Máximo da Verba.");
		}
		
	    Timestamp dtatual = getDtInicioDia(TimeUtils.getNow());
	    Collection<DynamicVO> verbasVO = verbaDAO.findAndOrderBy("CODPARC = ? AND DTINICIAL <= ? AND DTFINAL >= ? AND CODEMP = ? AND SALDOVERBA > 0", "DTFINAL", codparc, dtatual, dtatual, cabVO.asBigDecimal("CODEMP"));
	    
	    // Obtém o valor disponível de verba para o parceiro
	    for(DynamicVO verbaVO : verbasVO) {
	    	vlrDisponivelVerba = vlrDisponivelVerba.add(verbaVO.asBigDecimal("SALDOVERBA"));
	    }
	    
	    
	    
	    
	    /*JdbcWrapper jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);
        sql.setNamedParameter("P_CODPARC", codparc);
        sql.setNamedParameter("P_DATAATUAL", dtneg);
        sql.setNamedParameter("P_DTFINAL", dtneg);
        sql.setNamedParameter("P_CODEMP", cabVO.asBigDecimal("CODEMP"));
        ResultSet rs = sql.executeQuery("SELECT SUM(SALDOVERBA) AS SALDOVERBA "
        		+ " FROM AD_CONTROLEVERBAS "
        		+ " WHERE CODPARC = :P_CODPARC "
        		+ " AND DTINICIAL <= :P_DATAATUAL "
        		+ " AND DTFINAL >= :P_DTFINAL "
        		+ " AND CODEMP = :P_CODEMP "
        		+ " AND SALDOVERBA > 0 "
        		+ " AND TIPOVERBAS = 'S' ");*/
        /*ResultSet rs = sql.executeQuery("SELECT * "
        		+ " FROM AD_CONTROLEVERBAS "
        		+ " WHERE CODPARC = 1118 "
        		+ " AND DTINICIAL <= '18/03/2025' "
        		+ " AND DTFINAL >= '18/03/2025' "
        		+ " AND CODEMP = 1 "
        		+ " AND SALDOVERBA > 0 "
        		+ " AND TIPOVERBAS = 'S' ");*/
        // BigDecimal vlrVerba = BigDecimal.ZERO;

        /*if(rs.next()) {
        	vlrVerba = vlrVerba.add(rs.getBigDecimal("SALDOVERBA"));
        } */
        
        // vlrVerba = vlrVerba == null ? BigDecimal.ZERO : vlrVerba;
       // vlrDisponivelVerba = vlrVerba;
     // Se o valor da verba disponível for zero, a verba não é aplicada e sai da rotina
	   
	    if(vlrDisponivelVerba.compareTo(BigDecimal.ZERO) <= 0) {
	    	cabDAO.prepareToUpdate(cabVO)
			.set("AD_VALORVERBA", BigDecimal.ZERO)
			.set("AD_VERBAAPLICADA", BigDecimal.ONE)
			.update();
	    	
	    	return;
	    }
	    
	    // Verifica qual o valor máximo de desconto conforme o parâmetro PERCMAXVERBA e compara com o valor da verba disponível. 
	    // Se o valor máximo de desconto for maior que a verba disponível, então o desconto aplicado será a quantidade de verba disponível
	    // Caso contrário, o desconto aplicado será o valor máximo de desconto calculado.
	    //BigDecimal vlrMaxDesconto = vlrTotItens.multiply(percMax.divide(BigDecimal.valueOf(100))); //vlrTotItens.multiply(percMax.divide(BigDecimal.valueOf(100)));
		BigDecimal vlrMaxDesconto = cabVO.asBigDecimal("VLRNOTA").multiply(percMax.divide(BigDecimal.valueOf(100))); 
	    BigDecimal vlrVerbaAplicada = vlrMaxDesconto.compareTo(vlrDisponivelVerba) > 0 ? vlrDisponivelVerba : vlrMaxDesconto;
		BigDecimal percVerbaAplicada = vlrVerbaAplicada.divide(cabVO.asBigDecimal("VLRNOTA"), 5, RoundingMode.HALF_EVEN);
		
		cabDAO.prepareToUpdate(cabVO)
		.set("AD_VALORVERBA", vlrVerbaAplicada)
		.set("AD_VERBAAPLICADA", BigDecimal.ONE)
		.set("PERCDESC", percVerbaAplicada.multiply(BigDecimal.valueOf(100)))
		.update();
		
		BigDecimal verbaADescontar = vlrVerbaAplicada;
		
		recalculaNota(nunota);
		
		DynamicVO varVO = varDAO.findOne("NUNOTA = ?", nunota);
		BigDecimal nunotaorig = varVO.asBigDecimal("NUNOTAORIG");
		
		// Grava os históricos da verba
		for(DynamicVO verbaVO : verbasVO) {
			
	    	BigDecimal saldoVerba = verbaVO.asBigDecimal("SALDOVERBA");
	    	System.out.println("Saldo de Verba: " + saldoVerba);
	    	if(saldoVerba.compareTo(verbaADescontar) <= 0) {
	    		gravaLogVerba(verbaVO.asString("NUNICO"), cabVO, nunotaorig, saldoVerba);
	    		verbaADescontar = verbaADescontar.subtract(saldoVerba);
	    	} else {
	    		gravaLogVerba(verbaVO.asString("NUNICO"), cabVO, nunotaorig, verbaADescontar);
	    		verbaADescontar = BigDecimal.ZERO;
	    	}
	    	
	    	if(verbaADescontar.compareTo(BigDecimal.ZERO) <= 0) {
	    		break;
	    	}
	    	
	    }
		
	}
	
	public static void recalculaNota(BigDecimal nunota) throws Exception {
		JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
		DynamicVO cabVO = cabDAO.findByPK(nunota);
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity persistentEntityCab = dwfEntityFacade.findEntityByPrimaryKey(DynamicEntityNames.CABECALHO_NOTA, new Object[] { nunota});
		
		ImpostosHelpper imposto = new ImpostosHelpper();
		imposto.carregarNota(nunota);
		imposto.calculaICMS(true);
		
		imposto.totalizarNota(nunota);
		imposto.setForcarRecalculo(true);
		imposto.setAtualizaImpostos(true);
		imposto.setCalcularTudo(true);
		imposto.calcularImpostos(nunota);
		imposto.salvarNota();
		
        BigDecimal totalNota = imposto.calcularTotalNota(cabVO.asBigDecimal("NUNOTA"), imposto.calcularTotalItens(cabVO.asBigDecimal("NUNOTA"), false));
		
        cabVO.setProperty("VLRNOTA", totalNota);
        persistentEntityCab.setValueObject((EntityVO)cabVO);	
        
        JdbcWrapper JDBC = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
        NativeSql sql = new NativeSql(JDBC);
        String deleteFinanceiro = "DELETE FROM TGFFIN WHERE NUNOTA = "+cabVO.asBigDecimal("NUNOTA");
        sql.executeUpdate(deleteFinanceiro);
        CentralFinanceiro financeiroUtils = new CentralFinanceiro();
        financeiroUtils.inicializaNota(nunota);
        financeiroUtils.refazerFinanceiro();
		
	}
	
	private static void limpaHistoricoVerba(BigDecimal nunota) throws Exception {
		JapeWrapper histVerbaDAO = JapeFactory.dao("AD_CONTROLEVERBASHIST");
		JapeWrapper verbaDAO = JapeFactory.dao("AD_CONTROLEVERBAS");
		
		Collection<DynamicVO> histsVerbaVO = histVerbaDAO.find("NUNOTA = ?", nunota);
		
		for(DynamicVO histVO : histsVerbaVO) {
			BigDecimal saldoVerba = verbaDAO.findByPK(histVO.asString("NUNICO")).asBigDecimal("SALDOVERBA");
			
			verbaDAO.prepareToUpdateByPK(histVO.asString("NUNICO"))
			.set("SALDOVERBA", saldoVerba.add(histVO.asBigDecimal("VLRDESC")))
			.update();
			
			histVerbaDAO.delete(histVO.asString("NUNICO"), histVO.asBigDecimal("SEQ"));
		}
		
	}
	
	private static void gravaLogVerba(String nuverba, DynamicVO cabVO, BigDecimal nupedido, BigDecimal vlrDesc) throws Exception {
		JapeWrapper histVerbaDAO = JapeFactory.dao("AD_CONTROLEVERBASHIST");
		JapeWrapper verbaDAO = JapeFactory.dao("AD_CONTROLEVERBAS");
		
		Collection<DynamicVO> histsVO = histVerbaDAO.findAndOrderBy("NUNICO = ?", "SEQ", nuverba);
		BigDecimal seqHist = BigDecimal.ZERO;
		for(DynamicVO histVO : histsVO) {
			seqHist = histVO.asBigDecimal("SEQ");
		}
		seqHist = seqHist.add(BigDecimal.ONE);
		
		
		histVerbaDAO.create()
		.set("NUNICO", nuverba)
		.set("SEQ", seqHist)
		.set("NUNOTA", cabVO.asBigDecimal("NUNOTA"))
		.set("NUMNOTA", nupedido)
		.set("CODEMP", cabVO.asBigDecimal("CODEMP"))
		.set("CODPARC", cabVO.asBigDecimal("CODPARC"))
		.set("DTNEG", cabVO.asTimestamp("DTNEG"))
		.set("VLRNOTA", cabVO.asBigDecimal("VLRNOTA"))
		.set("VLRDESC", vlrDesc)
		.set("TIPOVERBA", BigDecimal.ONE)
		.save();
		
		verbaDAO.prepareToUpdateByPK(nuverba)
		.set("SALDOVERBA", verbaDAO.findByPK(nuverba).asBigDecimal("SALDOVERBA").subtract(vlrDesc))
		.update();
		
	}
	
	private static Timestamp getDtInicioDia(Timestamp data)  {
		Calendar calendar = Calendar.getInstance();
	    calendar.setTime(data);
	    calendar.set(Calendar.HOUR_OF_DAY, 0);
	    calendar.set(Calendar.MINUTE, 0);
	    calendar.set(Calendar.SECOND, 0);
	    calendar.set(Calendar.MILLISECOND, 0);
	    return new Timestamp(calendar.getTimeInMillis());
	}
	
	
	
}
