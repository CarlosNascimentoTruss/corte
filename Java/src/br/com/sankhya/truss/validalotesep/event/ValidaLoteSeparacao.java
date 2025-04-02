package br.com.sankhya.truss.validalotesep.event;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

public class ValidaLoteSeparacao implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent evt) throws Exception {
		// TODO Auto-generated method stub
		validaLoteSep(evt);
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
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent evt) throws Exception {
		// TODO Auto-generated method stub
		validaLoteSep(evt);
	}
	
	
	private static void validaLoteSep(PersistenceEvent evt) throws Exception {
		
		
		DynamicVO ampVO = (DynamicVO) evt.getVo();
		if(ampVO.asString("CONTROLEMP").equals(" ") || ampVO.asBigDecimal("QTD") == null || ampVO.asBigDecimal("QTD").equals(BigDecimal.ZERO)){
			return;
		}
		
		JapeWrapper prefDAO = JapeFactory.dao(DynamicEntityNames.PARAMETRO_SISTEMA);
		
		JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
		
		BigDecimal codlocal = prefDAO.findOne("CHAVE = ?", "LOCVALPESAGEM").asBigDecimal("INTEIRO");
		
		NativeSql q0 = new NativeSql(jdbc);
		q0.setNamedParameter("P_NUAPO", ampVO.asBigDecimal("NUAPO"));
		q0.setNamedParameter("P_SEQAPA", ampVO.asBigDecimal("SEQAPA"));
		q0.setNamedParameter("P_CODPRODMP", ampVO.asBigDecimal("CODPRODMP"));
		
		ResultSet r0 = q0.executeQuery("SELECT DISTINCT LMP.CODLOCALBAIXA\r\n"
				+ "FROM TPRAMP AMP\r\n"
				+ "JOIN TPRAPO APO ON APO.NUAPO = AMP.NUAPO\r\n"
				+ "JOIN TPRIATV IATV ON IATV.IDIATV = APO.IDIATV\r\n"
				+ "JOIN TPREFX EFX ON EFX.IDEFX = IATV.IDEFX\r\n"
				+ "JOIN TPRPRC PRC ON PRC.IDPROC = EFX.IDPROC\r\n"
				+ "JOIN TPRIPA IPA ON IPA.IDIPROC = IATV.IDIPROC\r\n"
				+ "JOIN TPRLMP LMP ON LMP.IDEFX = EFX.IDEFX AND LMP.CODPRODPA = IPA.CODPRODPA \r\n"
				+ "WHERE AMP.NUAPO = :P_NUAPO AND AMP.SEQAPA = :P_SEQAPA AND LMP.CODPRODMP = :P_CODPRODMP");
		
		if(r0.next()) {
			codlocal = r0.getBigDecimal("CODLOCALBAIXA");
		}
		
		NativeSql q1 = new NativeSql(jdbc);
		q1.setNamedParameter("P_CODPROD", ampVO.asBigDecimal("CODPRODMP"));
		q1.setNamedParameter("P_CONTROLE", ampVO.asString("CONTROLEMP"));
		ResultSet r1 = q1.executeQuery("SELECT MAX(CODLOCAL) AS CODLOCAL \r\n"
				+ "FROM AD_TRASETIQUETA \r\n"
				+ "WHERE CODPROD = :P_CODPROD \r\n"
				+ "AND CONTROLE = :P_CONTROLE ");
		
		if(r1.next()) {
			if (!codlocal.equals(r1.getBigDecimal("CODLOCAL"))) {
				throw new Error("<b>Local da etiqueta é diferente do local de baixa cadastrado do produto. Verifique o cadastro.</b>");
			}
		}
		
		
		NativeSql q = new NativeSql(jdbc);
		q.setNamedParameter("P_CODEMP", BigDecimal.valueOf(6));
		q.setNamedParameter("P_CODPROD", ampVO.asBigDecimal("CODPRODMP"));
		q.setNamedParameter("P_CODLOCAL", codlocal);
		q.setNamedParameter("P_CONTROLE", ampVO.asString("CONTROLEMP"));
		ResultSet r = q.executeQuery("SELECT NVL(SUM(ESTOQUE),0) AS ESTOQUE "
				+ " FROM TGFEST "
				+ " WHERE CODEMP = :P_CODEMP "
				+ " AND CODPROD = :P_CODPROD "
				+ " AND CODLOCAL = :P_CODLOCAL "
				+ " AND CODPARC = 0 "
				+ " AND CONTROLE = :P_CONTROLE ");
		
		
		
		if(r.next()) {
			BigDecimal estoque = r.getBigDecimal("ESTOQUE");
			
			if(estoque.compareTo(ampVO.asBigDecimal("QTD")) < 0) {
				if (estoque.compareTo(BigDecimal.ZERO) > 0) {
					ampVO.setProperty("QTD", estoque);
				} else {
					throw new Error("<b>Quantidade do produto na etiqueta não disponível em estoque na empresa 6 no local "+ codlocal +". Favor realizar a conferência.</b>");
				}
			}
		}
	}
}
