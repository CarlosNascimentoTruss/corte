package br.com.sankhya.truss.inventario;



import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.StringUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class validaIteinventEtiqueta implements EventoProgramavelJava {

	static BigDecimal qtdEmb = BigDecimal.ZERO;
	static String codBarrasEtiqueta;
	static BigDecimal codProd;
	static BigDecimal codEtiq;

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		String acao = "U";
		gerenciador(arg0, acao);

	}

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		String acao = "I";
		gerenciador(arg0, acao);

	}

	public static void gerenciador(PersistenceEvent arg0, String acao) throws Exception {
		System.out.println("validaIteinventEtiqueta.gerenciador()");
		System.out.println("//////////// Evento na Tabela => AD_PALETINVENT ////////////");

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(jdbc);

		DynamicVO newVO = (DynamicVO) arg0.getVo();

		BigDecimal nuInvent = BigDecimalUtil.getValueOrZero((BigDecimal) newVO.getProperty("NUINVENT"));
		BigDecimal qtdProd = BigDecimalUtil.getValueOrZero((BigDecimal) newVO.getProperty("QTDPROD"));
		String codBarras = StringUtils.getValueOrDefault((String) newVO.getProperty("CODBARRAS"), "X");
		
		System.out.println("nuInvent: " + nuInvent);
		System.out.println("qtdProd: " + qtdProd);
		System.out.println("codBarras: " + codBarras);
		
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT  QTDEMB, CODBARRAS ");
		sql.append(" FROM AD_TRASETIQUETA ETA ");
		sql.append(
				" WHERE CODBARRAS IN (SELECT CODBARRASETIQUETA FROM ad_trastreabilidade WHERE CODBARRASETIQUETAPALETE = '"
						+ codBarras + "') AND QTDEMB = 0 ");
		ResultSet query1 = nativeSql.executeQuery(sql.toString());
		System.out.println("query1: " + sql.toString());
		
		String msgEtiquetasZeradas = "";
		int qtdEtiquetaZerada = 0;
		
		while(query1.next()) {
			qtdEtiquetaZerada++;
			msgEtiquetasZeradas = msgEtiquetasZeradas + "<br>\n " + query1.getString("CODBARRAS");
		}
		
		if(qtdEtiquetaZerada > 0) {
			throw new MGEModelException(
					"Existem etiquetas com quantidades iguais a zero. <br>\n Etiquetas Zeradas: <br>\n " + msgEtiquetasZeradas);
		}
		
			/*
		if (query1.next()) {
			throw new MGEModelException(
					"Existem etiquetas com quantidades iguais a zero. Necessário bipar as caixas individualmente.");
		}
		*/
		JapeWrapper qtdembDAO = JapeFactory.dao("AD_TRASTREABILIDADE");
		DynamicVO qtdembVO = qtdembDAO.findOne(" CODBARRASETIQUETAPALETE = '" + codBarras + "'");

		if (qtdembVO == null) {
			/*throw new MGEModelException(
					"Existem etiquetas com quantidades iguais a zero. Necessário bipar as caixas individualmente.");*/
			throw new MGEModelException(
					"Não existe etiquetas caixa vinculadas a esta etiqueta palete.");

		}

	}

	/*
	 * StringBuilder sql2 = new StringBuilder(); sql2.
	 * append(" SELECT COUNT(*) AS QTDEMB FROM AD_TRASTREABILIDADE WHERE CODBARRASETIQUETAPALETE = '"
	 * + codBarras + "') "); ResultSet query2 =
	 * nativeSql.executeQuery(sql2.toString()); System.out.println("query2: " +
	 * sql2.toString());
	 * 
	 * if (query2.next()) {
	 * 
	 * BigDecimal qtdEmb2 = query2.getBigDecimal("QTDEMB");
	 * 
	 * if (qtdEmb2.compareTo(BigDecimal.ZERO) == 0) {
	 * 
	 * exibirMensagem(); throw new MGEModelException(
	 * "Existem etiquetas com quantidades iguais a zero. Necessário bipar as caixas individualmente."
	 * ); } }
	 */

	// 90UC0002302479
	/*
	 * else {
	 * 
	 * while (query1.next()) { qtdEmb =
	 * BigDecimalUtil.getValueOrZero(query1.getBigDecimal("QTDEMB"));
	 * codBarrasEtiqueta =
	 * StringUtils.getValueOrDefault(query1.getString("CODBARRAS"), " "); codEtiq =
	 * BigDecimalUtil.getValueOrZero(query1.getBigDecimal("CODETIQ")); codProd =
	 * BigDecimalUtil.getValueOrZero(query1.getBigDecimal("CODPROD"));
	 * 
	 * System.out.println("qtdEmb: " + qtdEmb);
	 * 
	 * if (qtdEmb.compareTo(BigDecimal.ZERO) == 0) {
	 * 
	 * throw new MGEModelException(
	 * "Existem etiquetas com quantidades iguais a zero. Necessário bipar as caixas individualmente."
	 * ); } }
	 * 
	 * }
	 

	public static void exibirMensagem() throws IOException {
		throw new IOException(
				"Existem etiquetas com quantidades iguais a zero. Necessário bipar as caixas individualmente.");
	}
	*/
}
