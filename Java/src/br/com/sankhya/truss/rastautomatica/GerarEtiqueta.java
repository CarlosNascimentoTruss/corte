package br.com.sankhya.truss.rastautomatica;


import java.math.BigDecimal;
import java.sql.ResultSet;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.StringUtils;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class GerarEtiqueta implements AcaoRotinaJava {

	private static SessionHandle hnd = null;
	private static JdbcWrapper jdbc = null;
	private static Boolean isOpen = Boolean.FALSE;
	private String MsgErro = "";

	BigDecimal usuarioLogado = null;
	String nroOp;
	String nroUnico;
	String codProd;
	int qtdEtiq = 0;
	String controle;
	String codEmp;
	int countCaixa = 0;
	String nroLote;
	int ultCod = 0;
	BigDecimal codLocal;

	public void doAction(ContextoAcao ctx) throws Exception {
		System.out.println("GerarEtiqueta.doAction()");

		if (!ctx.confirmarSimNao("Confirma", "Deseja prosseguir?", 1))
			return;

		System.out.println("GerarEtiqueta.doAction()");

		//for (int i = 0; i < ctx.getLinhas().length; i++) {
			//Registro line = ctx.getLinhas()[i];
			try {
				gerenciador(ctx);
			} catch (Exception e) {
				e.printStackTrace();
			}
	//	}

		if (MsgErro.equals("")) {

			ctx.setMensagemRetorno("Etiqueta gerada com sucesso.");
			/* + LancarTela.abrirTela(nufin, LancarTela.Telas.FINANCEIRO, "NUFIN") */

		} else {
			ctx.setMensagemRetorno(MsgErro);
		}

	}

	private void gerenciador(ContextoAcao ctx) throws Exception {

		System.out.println("GerarEtiqueta.gerenciador()");

		try {
			openSession();
			usuarioLogado = ctx.getUsuarioLogado();

			System.out.println("AQUI4:");
			nroOp = StringUtils.getValueOrDefault((String) ctx.getParam("NROP"), "X");
			System.out.println("nroOp: " + nroOp);
			nroUnico = StringUtils.getValueOrDefault((String) ctx.getParam("NUNOTA"), "X");
			System.out.println("nroUnico: " + nroUnico);
			codProd = StringUtils.getValueOrDefault((String) ctx.getParam("CODPROD"), "X");
			System.out.println("codProd: " + codProd);
			qtdEtiq = (int) ctx.getParam("QTDETIQ");
			System.out.println("qtdEtiq: " + qtdEtiq);
			controle = StringUtils.getValueOrDefault((String) ctx.getParam("CONTROLE"), " ");
			System.out.println("controle: " + controle);
			codEmp = StringUtils.getValueOrDefault((String) ctx.getParam("CODEMP"), "X");
			System.out.println("codEmp: " + codEmp);

			gerarEtiqueta();

		} catch (Exception e) {
			e.printStackTrace();
			MsgErro = e.getMessage();
		} finally {
			closeSession();
		}
	}

	private void gerarEtiqueta() throws Exception {
		System.out.println("GerarEtiqueta.gerarEtiqueta()");

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(jdbc);

		if (!nroOp.equals("X")) {

			StringBuilder sql = new StringBuilder();
			sql.append(" SELECT IPA.NROLOTE   ");
			sql.append(" FROM TPRIPA IPA ");
			sql.append(" INNER JOIN TGFPRO PRO ON (PRO.CODPROD = IPA.CODPRODPA) ");
			sql.append("  WHERE IPA.IDIPROC = " + new BigDecimal(nroOp));
			sql.append(" AND IPA.CODPRODPA = " + new BigDecimal(codProd));
			ResultSet query1 = nativeSql.executeQuery(sql.toString());
			System.out.println("query1: " + sql.toString());
			if (query1.next()) {
				nroLote = StringUtils.getEmptyAsNull(query1.getString("NROLOTE"));
				System.out.println("nroLote: " + nroLote);
			}

			StringBuilder sql3 = new StringBuilder();
			sql3.append(" SELECT NVL(MAX(NROCAIXA),0) + 1 AS COUNTCAIXA ");
			sql3.append(" FROM AD_TRASETIQUETA  E ");
			sql3.append("  WHERE E.CONTROLE = '" + controle + "'");
			sql3.append("  AND CODPROD = " + new BigDecimal(codProd));

			ResultSet query3 = nativeSql.executeQuery(sql3.toString());
			System.out.println("query3: " + sql3.toString());
			if (query3.next()) {
				countCaixa = (int) query3.getInt("COUNTCAIXA");// BigDecimalUtil.getValueOrZero((BigDecimal)
																// query3.getBigDecimal("COUNTCAIXA"));
				System.out.println("countCaixa: " + countCaixa);

			}

			System.out.println("Antes de Entrar no segundo IF");
		} else if (!nroUnico.equals("X") && !controle.equals(" ")) {

			System.out.println("Entrou no segundo IF");

			//nroOp = nroUnico;

			StringBuilder sqlNota = new StringBuilder();
			sqlNota.append(" SELECT I.CONTROLE   ");
			sqlNota.append(" FROM TGFITE I ");
			sqlNota.append(" JOIN TGFCAB C ON C.NUNOTA = I.NUNOTA ");
			sqlNota.append("  WHERE C.NUNOTA  = " + new BigDecimal(nroUnico));
			sqlNota.append(" AND I.CODPROD  = " + new BigDecimal(codProd));
			sqlNota.append(" AND C.CODEMP = " + new BigDecimal(codEmp));
			sqlNota.append(" AND C.TIPMOV = 'C' ");
			sqlNota.append(" AND C.STATUSNOTA = 'L' ");
			sqlNota.append(" AND I.CONTROLE = '" + controle + "'");

			ResultSet queryNota = nativeSql.executeQuery(sqlNota.toString());
			System.out.println("queryNota: " + sqlNota.toString());
			if (queryNota.next()) {
				nroLote = StringUtils.getEmptyAsNull(queryNota.getString("CONTROLE"));
				System.out.println("nroLote: " + nroLote);
			}

			StringBuilder sqlNota2 = new StringBuilder();
			sqlNota2.append(" SELECT COUNT(1) + 1 AS COUNTCAIXA ");
			sqlNota2.append(" FROM AD_TRASETIQUETA  E ");
			sqlNota2.append("  WHERE E.NUNOTA = " + new BigDecimal(nroUnico));
			sqlNota2.append("  AND CODPROD = " + new BigDecimal(codProd));
			sqlNota2.append("  AND TIPOPROD  = 'MP' ");
			sqlNota2.append("  AND NVL(SITUACAO, '-') <> 'CAN' ");
			ResultSet query2 = nativeSql.executeQuery(sqlNota2.toString());
			System.out.println("query2: " + sqlNota2.toString());
			if (query2.next()) {
				countCaixa = (int) query2.getInt("COUNTCAIXA");
				System.out.println("countCaixa: " + countCaixa);
			}

		} else {

			throw new MGEModelException("Necesssário informar o Nro. OP ou Nro. Único da Nota e Nro. do Lote.");

		}

		StringBuilder sql2 = new StringBuilder();
		sql2.append(" SELECT CODLOCALPADRAO   ");
		sql2.append(" FROM TGFPRO ");
		sql2.append("  WHERE CODPROD = " + new BigDecimal(codProd));
		ResultSet query2 = nativeSql.executeQuery(sql2.toString());
		System.out.println("query2: " + sql2.toString());
		if (query2.next()) {
			codLocal = BigDecimalUtil.getValueOrZero((BigDecimal) query2.getBigDecimal("CODLOCALPADRAO"));
			System.out.println("codLocal: " + codLocal);

		}

		StringBuilder sql4 = new StringBuilder();
		sql4.append(" SELECT N.ULTCOD ");
		sql4.append(" FROM TGFNUM N ");
		sql4.append("  WHERE N.ARQUIVO = 'AD_TRASETIQUETA' ");
		ResultSet query4 = nativeSql.executeQuery(sql4.toString());
		System.out.println("query4: " + sql4.toString());
		if (query4.next()) {
			ultCod = (int) query4.getInt("ULTCOD");
			System.out.println("countCaixa: " + ultCod);
		}

		for (int c1 = 1; c1 <= qtdEtiq; c1++) {
			System.out.println("Executando lógica para o número: " + c1);

			ultCod = ultCod + 1;

			if (!nroOp.equals("X")) {
				JapeWrapper etiquetaDAO = JapeFactory.dao("AD_TRASETIQUETA");
				DynamicVO save = etiquetaDAO.create().set("CODETIQ", BigDecimal.valueOf(ultCod))
						.set("CODPROD", new BigDecimal(codProd)).set("TIPOPROD", "PA").set("CONTROLE", nroLote)
						.set("SITUACAO", "NAO").set("CODUSUINCLUSAO", usuarioLogado)
						.set("DHINCLUSAO", TimeUtils.getNow()).set("NUOP", new BigDecimal(nroOp))
						.set("NROCAIXA", BigDecimal.valueOf(countCaixa)).set("GERARMAIOR", "S")
						.set("CODLOCAL", codLocal).set("CODEMP", new BigDecimal(codEmp))
						.set("OBSERVACAO", "Reimpressão Inventário").save();
			}
			if (!nroUnico.equals("X") && !controle.equals(" ")) {

				JapeWrapper etiquetaDAO = JapeFactory.dao("AD_TRASETIQUETA");
				DynamicVO save = etiquetaDAO.create().set("CODETIQ", BigDecimal.valueOf(ultCod))
						.set("CODPROD", new BigDecimal(codProd)).set("TIPOPROD", "MP").set("CONTROLE", nroLote)
						.set("SITUACAO", "NAO").set("CODUSUINCLUSAO", usuarioLogado)
						.set("DHINCLUSAO", TimeUtils.getNow()).set("NUNOTA", new BigDecimal(nroUnico))
						.set("NROCAIXA", BigDecimal.valueOf(countCaixa)).set("GERARMAIOR", "S")
						.set("CODLOCAL", codLocal).set("CODEMP", new BigDecimal(codEmp))
						.set("OBSERVACAO", "Reimpressão Inventário").save();

			}

			countCaixa = countCaixa + 1;

		}

		nativeSql.executeUpdate(" update tgfnum set ultcod = " + ultCod + " where arquivo = 'AD_TRASETIQUETA' ");

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
		if (isOpen) {
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
			isOpen = false;
			jdbc = null;
		}
	}
}
