package br.com.sankhya.truss.verba;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.CentralFinanceiro;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class AplicaVerbaCabEvent implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent ctx) throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void afterInsert(PersistenceEvent ctx) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(PersistenceEvent ctx) throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void beforeInsert(PersistenceEvent ctx) throws Exception {
		// TODO Auto-generated method stub
		if("S".equals(JapeSession.getPropertyAsString("br.com.sankhya.mgecom.centralnotas.NotaSendoCancelada"))){ 
			return;
		}
			aplicaVerba(ctx,"I");
	}

	@Override
	public void beforeUpdate(PersistenceEvent ctx) throws Exception {
		// TODO Auto-generated method stub
		// Evento aplicável apenas para a tabela TGFCAB
		if("S".equals(JapeSession.getPropertyAsString("br.com.sankhya.mgecom.centralnotas.NotaSendoCancelada"))){ 
			return;
		}
		aplicaDescontoUpd(ctx);
	}
	
	private static BigDecimal aplicaVerba(PersistenceEvent ctx, String tipo) throws Exception {
		
		
		
		DynamicVO newVO = (DynamicVO) ctx.getVo();
		JapeWrapper verbaDAO = JapeFactory.dao("AD_CONTROLEVERBAS");
		JapeWrapper prefDAO = JapeFactory.dao(DynamicEntityNames.PARAMETRO_SISTEMA);
		JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
		
		BigDecimal nunota = newVO.asBigDecimal("NUNOTA");
		
		DynamicVO cabVO = (DynamicVO) ctx.getVo();
		
		if(!cabVO.asString("TIPMOV").equals("V")) {
			return BigDecimal.ZERO;
		}
		/*if ("U".equals(tipo)) {
			cabVO.setProperty("VLRDESCTOT", BigDecimal.ZERO);
			recalculaNota(cabVO);
		}*/
		
		// Faz a restauração da verba antes de fazer um novo cálculo. Ação necessária por ser executada em cada item.
		limpaHistoricoVerba(nunota);
		
		
		BigDecimal codparc = cabVO.asBigDecimal("CODPARC");
		BigDecimal vlrDisponivelVerba = BigDecimal.ZERO;
		BigDecimal percMax = prefDAO.findOne("CHAVE = ?", "PERCMAXVERBA").asBigDecimal("NUMDEC");
		
		if(percMax == null) {
			throw new Exception("Não foi possível calcular a verba. Informe um valor para o parâmetro Percentual Máximo da Verba.");
		}
		
	    Timestamp dtatual = getDtInicioDia(TimeUtils.getNow());
	    Collection<DynamicVO> verbasVO = verbaDAO.findAndOrderBy("CODPARC = ? AND DTINICIAL <= ? AND DTFINAL >= ? AND CODEMP = ? AND SALDOVERBA > 0", "DTFINAL", codparc, dtatual, dtatual, cabVO.asBigDecimal("CODEMP"));
	    
	    // Obtém o valor disponível de verba para o parceiro
	    for(DynamicVO verbaVO : verbasVO) {
	    	vlrDisponivelVerba = vlrDisponivelVerba.add(verbaVO.asBigDecimal("SALDOVERBA"));
	    }
	    
	    // Se o valor da verba disponível for zero, a verba não é aplicada e sai da rotina
	    if(vlrDisponivelVerba.compareTo(BigDecimal.ZERO) <= 0) {
	    	cabVO.setProperty("AD_VALORVERBA", BigDecimal.ZERO);
	    	cabVO.setProperty("AD_VERBAAPLICADA", BigDecimal.ONE);
	    	return BigDecimal.ZERO;
	    }
	    
	    Collection<DynamicVO> itesVO = iteDAO.find("NUNOTA = ?", nunota);
		
		BigDecimal valorItens = BigDecimal.ZERO;
		
		for(DynamicVO iteVO : itesVO) {
			valorItens = valorItens.add(iteVO.asBigDecimal("VLRTOT").add(iteVO.asBigDecimal("VLRIPI").add(iteVO.asBigDecimal("VLRSUBST"))));
		}
		
		BigDecimal valorBase = "U".equals(tipo) ? valorItens : cabVO.asBigDecimal("VLRNOTA");
	    
	    // Verifica qual o valor máximo de desconto conforme o parâmetro PERCMAXVERBA e compara com o valor da verba disponível. 
	    // Se o valor máximo de desconto for maior que a verba disponível, então o desconto aplicado será a quantidade de verba disponível
	    // Caso contrário, o desconto aplicado será o valor máximo de desconto calculado.
	    BigDecimal vlrMaxDesconto = valorBase.multiply(percMax.divide(BigDecimal.valueOf(100))); //vlrTotItens.multiply(percMax.divide(BigDecimal.valueOf(100)));
		BigDecimal vlrVerbaAplicada = vlrMaxDesconto.compareTo(vlrDisponivelVerba) > 0 ? vlrDisponivelVerba : vlrMaxDesconto;
		BigDecimal percVerbaAplicada = vlrVerbaAplicada.divide(valorBase, 5, RoundingMode.HALF_EVEN);
		
		
		
		
		/*if(1==1) {
					throw new Exception("AD_VLRVERBA: " + vlrVerbaAplicada + "\n" +
										"VLRNOTA: " + cabVO.asBigDecimal("VLRNOTA") + "\n" + 
							            "vlrMaxDesconto: " + vlrMaxDesconto + "\n" + 
										"valorItens: " + valorItens);
				}
		*/
		
		cabVO.setProperty("AD_VALORVERBA", vlrVerbaAplicada);
		cabVO.setProperty("AD_VLRVERBA", vlrVerbaAplicada);
		cabVO.setProperty("AD_VERBAAPLICADA", BigDecimal.ONE);
		cabVO.setProperty("PERCDESC", percVerbaAplicada.multiply(BigDecimal.valueOf(100)));
		
		BigDecimal verbaADescontar = vlrVerbaAplicada;
		
		
		
		// Grava os históricos da verba
		for(DynamicVO verbaVO : verbasVO) {
			
	    	BigDecimal saldoVerba = verbaVO.asBigDecimal("SALDOVERBA");
	    	System.out.println("Saldo de Verba: " + saldoVerba);
	    	if(saldoVerba.compareTo(verbaADescontar) <= 0) {
	    		gravaLogVerba(verbaVO.asString("NUNICO"), cabVO, BigDecimal.ZERO, saldoVerba);
	    		verbaADescontar = verbaADescontar.subtract(saldoVerba);
	    	} else {
	    		gravaLogVerba(verbaVO.asString("NUNICO"), cabVO, BigDecimal.ZERO, verbaADescontar);
	    		verbaADescontar = BigDecimal.ZERO;
	    	}
	    	
	    	if(verbaADescontar.compareTo(BigDecimal.ZERO) <= 0) {
	    		break;
	    	}
	    }
		
		return cabVO.asBigDecimal("AD_VLRVERBA");
	}
	
	/* 
	 * Método para obter o valor colocado em AD_VLRVERBA calculado no insert da nota e incluir no campo nativo VLRDESCTOT
	 * Ação necessária pois a inserção deste campo no insert da TGFCAB não surte efeito
	*/
	private void aplicaDescontoUpd(PersistenceEvent ctx) throws Exception {
		DynamicVO cabVO = (DynamicVO) ctx.getVo();
		DynamicVO oldCabVO = (DynamicVO) ctx.getOldVO();
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		BigDecimal vlrVerba = cabVO.asBigDecimalOrZero("AD_VLRVERBA");
		
		if(!cabVO.asString("TIPMOV").equals("V")) {
			return;
		}
		
		  if(!cabVO.asBigDecimalOrZero("VLRNOTA").equals(oldCabVO.asBigDecimalOrZero("VLRNOTA")) 
				//&& cabVO.asBigDecimalOrZero("VLRDESCTOT").equals(oldCabVO.asBigDecimalOrZero("VLRDESCTOT")) 
				&& !cabVO.asBigDecimalOrZero("VLRDESCTOT").equals(BigDecimal.ZERO) 
				&& cabVO.asBigDecimalOrZero("VLRDESCTOT").divide(cabVO.asBigDecimalOrZero("VLRNOTA"), 5).compareTo(BigDecimal.valueOf(0.3)) > 0 ) {
			  
			cabVO.setProperty("VLRDESCTOT", BigDecimal.ZERO);
			dwfEntityFacade.saveEntity(DynamicEntityNames.CABECALHO_NOTA, (EntityVO) cabVO);
			//recalculaNota(cabVO);
			vlrVerba = aplicaVerba(ctx, "U");
			
			if(1==1) {
				  throw new Exception("VlrVerba: " + vlrVerba + " VlrNota: " + cabVO.asBigDecimalOrZero("VLRNOTA"));
			  }
		}
		
		
		  
		if(BigDecimal.ZERO.equals(cabVO.asBigDecimalOrZero("VLRDESCTOT")) && !BigDecimal.ZERO.equals(vlrVerba) && !BigDecimal.ZERO.equals(cabVO.asBigDecimalOrZero("VLRNOTA")) ) {
			cabVO.setProperty("VLRDESCTOT", vlrVerba.setScale(2, RoundingMode.HALF_EVEN));
			cabVO.setProperty("PERCDESC", (vlrVerba.divide(cabVO.asBigDecimal("VLRNOTA"), 4, RoundingMode.HALF_EVEN)).multiply(BigDecimal.valueOf(100)));
			recalculaNota(cabVO);
		}
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
	
	public static void recalculaNota(DynamicVO cabVO) throws Exception {
		BigDecimal nunota = cabVO.asBigDecimal("NUNOTA");
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity persistentEntityCab = dwfEntityFacade.findEntityByPrimaryKey(DynamicEntityNames.CABECALHO_NOTA, new Object[] { nunota});
		
		
		ImpostosHelpper imposto = new ImpostosHelpper();
		
		imposto.totalizarNota(nunota);
		imposto.referenciarNota(cabVO, persistentEntityCab);
		imposto.calcularImpostos(nunota);
		
		
		imposto.carregarNota(nunota);
		imposto.calculaICMS(true);
		
		imposto.totalizarNota(nunota);
		imposto.setForcarRecalculo(true);
		imposto.setAtualizaImpostos(true);
		imposto.setCalcularTudo(true);
		imposto.calcularImpostos(nunota);
		imposto.salvarNota();
		
        BigDecimal totalNota = imposto.calcularTotalNota(cabVO.asBigDecimal("NUNOTA"), imposto.calcularTotalItens(cabVO.asBigDecimal("NUNOTA"), false));

        CentralFinanceiro financeiroUtils = new CentralFinanceiro();
        financeiroUtils.inicializaNota(nunota);
        financeiroUtils.refazerFinanceiro();
         
        // Campo AD_VLRVERBA é setado para zero para que a atualização não seja feita novamente
        cabVO.setProperty("AD_VLRVERBA", BigDecimal.ZERO);
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
