
package br.com.sankhya.truss.corte.actions;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class LiberaItensExpedicao

		implements AcaoRotinaJava

{

	private static JapeSession.SessionHandle hnd = null;

	private static JdbcWrapper jdbc = null;

	private static Boolean isOpen = Boolean.FALSE;

	private static String mensagemErro = "";

	public void doAction(ContextoAcao ctx) throws Exception {

		try {

			openSession();

			JapeWrapper proDAO = JapeFactory.dao("Produto");

			JapeWrapper cabDAO = JapeFactory.dao("CabecalhoNota");

			JapeWrapper iteDAO = JapeFactory.dao("ItemNota");

			Registro[] linhas = ctx.getLinhas();

			String liberaIntegral = (String) ctx.getParam("P_LIBERAINTEGRAL");

			BigDecimal qtdLiberar = ((Double) ctx.getParam("P_QTDLIBERAR") == null) ? BigDecimal.ZERO : new BigDecimal(((Double) ctx.getParam("P_QTDLIBERAR")).doubleValue());

			if ("S".equals(liberaIntegral)) {

				BigDecimal bigDecimal = (BigDecimal) linhas[0].getCampo("NUNOTA");

				liberaIntegral(bigDecimal);

				ctx.setMensagemRetorno(mensagemErro.equals("") ? "Itens Liberados." : mensagemErro);

				return;

			}

			if (!"S".equals(liberaIntegral) && qtdLiberar.equals(BigDecimal.ZERO)) {

				ctx.mostraErro("Se não liberar o pedido integralmente, um valor de itens liberados deve ser informado.");

			}

			BigDecimal nunota = null;
			byte b;
			int i;

			Registro[] arrayOfRegistro1;

			for (i = (arrayOfRegistro1 = linhas).length, b = 0; b < i;) {
				Registro linha = arrayOfRegistro1[b];

				nunota = (BigDecimal) linha.getCampo("NUNOTA");

				BigDecimal qtdConferida = (BigDecimal) linha.getCampo("QTDCONFERIDA");

				BigDecimal codprod = (BigDecimal) linha.getCampo("CODPROD");

				BigDecimal codemp = (BigDecimal) linha.getCampo("CODEMP");

				BigDecimal qtdneg = (BigDecimal) linha.getCampo("QTDNEG");

				BigDecimal codlocalorig = (BigDecimal) linha.getCampo("CODLOCALORIG");

				BigDecimal disponivel = BigDecimal.ZERO;

				BigDecimal qtdMinVenda = proDAO.findByPK(new Object[] { codprod }).asBigDecimalOrZero("AD_QTDMINVENDA");

				if (qtdMinVenda.compareTo(BigDecimal.ZERO) <= 0) {

					ctx.mostraErro("Produto sem quantidade múltipla de venda cadastrado.");

				}

				if (!isMultiplo(qtdLiberar, qtdMinVenda)) {

					ctx.mostraErro("Indique uma quantidade múltipla da qtd. múltipla de venda do produto: " + qtdMinVenda);

				}

				if (qtdLiberar.compareTo(qtdConferida) > 0) {

					ctx.mostraErro("Quantidade informada maior que a quantidade cortada.");

				}

				NativeSql queryEstLocal = new NativeSql(jdbc);

				queryEstLocal.setNamedParameter("P_CODPROD", codprod);

				queryEstLocal.setNamedParameter("P_CODEMP", codemp);

				queryEstLocal.setNamedParameter("P_CODLOCAL", codlocalorig);

				ResultSet rsEstLocal = queryEstLocal
						.executeQuery("SELECT SUM(DISPONIVEL) AS DISPONIVEL FROM AD_VW_ESTOQUELOCAL  WHERE CODPROD = :P_CODPROD  AND CODEMP IN (:P_CODEMP, 0)  AND CODLOCAL = :P_CODLOCAL ");

				NativeSql queryEstoqueTerceiros = new NativeSql(jdbc);

				queryEstoqueTerceiros.setNamedParameter("P_CODPROD", codprod);

				queryEstoqueTerceiros.setNamedParameter("P_CODEMP", codemp);

				queryEstoqueTerceiros.setNamedParameter("P_CODLOCAL", codlocalorig);

				ResultSet rsEstoqueTerceiros = queryEstoqueTerceiros.executeQuery(
						"SELECT SUM(ESTOQUE - RESERVADO) AS ESTOQUETERC  FROM TGFEST EST  WHERE EST.CODPROD = :P_CODPROD  AND EST.CODEMP = :P_CODEMP  AND EST.CODLOCAL = :P_CODLOCAL  AND CODPARC IN (SELECT CODPARC FROM AD_TERCCORTEGLOBAL) ");

				BigDecimal estoqueTerceiros = BigDecimal.ZERO;

				if (rsEstoqueTerceiros.next()) {

					estoqueTerceiros = rsEstoqueTerceiros.getBigDecimal("ESTOQUETERC");

				}

				if (rsEstLocal.next()) {

					disponivel = rsEstLocal.getBigDecimal("DISPONIVEL").add(qtdneg).subtract(estoqueTerceiros);

				}

				if (qtdLiberar.compareTo(disponivel) > 0) {

					ctx.mostraErro("Não existe quantidade disponível suficiente para liberar a quantidade solicitada. \n Quantidade Disponível :" + disponivel);

				}

				linha.setCampo("QTDCONFERIDA", qtdConferida.subtract(qtdLiberar));

				b++;
			}

			String status = "20";

			for (DynamicVO iteVO : iteDAO.find("NUNOTA = ?", new Object[] { nunota })) {

				if (iteVO.asBigDecimal("QTDCONFERIDA").compareTo(BigDecimal.ZERO) > 0) {

					status = "19";

				}

			}

			((FluidUpdateVO) cabDAO.prepareToUpdateByPK(new Object[] { nunota

			}).set("AD_STATUSPED", status))

					.update();

			ctx.setMensagemRetorno(mensagemErro.equals("") ? "Item liberado com sucesso." : mensagemErro);

		}

		catch (Exception e) {

			e.printStackTrace();

			ctx.mostraErro(e.getMessage());

		} finally {

			closeSession();

		}

	}

	private void liberaIntegral(BigDecimal nunota) throws Exception {

		JapeWrapper iteDAO = JapeFactory.dao("ItemNota");

		JapeWrapper cabDAO = JapeFactory.dao("CabecalhoNota");

		int count = 0;

		String continua = "";

		for (DynamicVO iteVO : iteDAO.find("NUNOTA = ? AND QTDCONFERIDA > 0", new Object[] { nunota })) {

			continua = verificaEstoque(iteVO.asBigDecimal("CODPROD"),

					iteVO.asBigDecimal("CODEMP"),

					iteVO.asBigDecimal("CODLOCALORIG"),

					iteVO.asBigDecimal("QTDCONFERIDA"),

					iteVO.asBigDecimal("QTDNEG"));

			if (continua.equals("S")) {

				((FluidUpdateVO) iteDAO.prepareToUpdate(iteVO)

						.set("QTDCONFERIDA", BigDecimal.ZERO))

						.update();
				continue;

			}

			count++;

			mensagemErro = "Itens Liberados. Alguns itens não puderam ser liberados por falta de estoque. Verifique o campo Qtd. Corte na aba de itens.";

		}

		if ("N".equals(continua)) {

			((FluidUpdateVO) cabDAO.prepareToUpdateByPK(new Object[] { nunota

			}).set("AD_STATUSPED", "19"))

					.update();

		} else {

			((FluidUpdateVO) cabDAO.prepareToUpdateByPK(new Object[] { nunota

			}).set("AD_STATUSPED", "20"))

					.update();

		}

	}

	private String verificaEstoque(BigDecimal codprod, BigDecimal codemp, BigDecimal codlocalorig, BigDecimal qtdLiberar, BigDecimal qtdneg) throws Exception {

		NativeSql queryEstLocal = new NativeSql(jdbc);

		queryEstLocal.setNamedParameter("P_CODPROD", codprod);

		queryEstLocal.setNamedParameter("P_CODEMP", codemp);

		queryEstLocal.setNamedParameter("P_CODLOCAL", codlocalorig);

		BigDecimal disponivel = BigDecimal.ZERO;

		ResultSet rsEstLocal = queryEstLocal
				.executeQuery("SELECT SUM(DISPONIVEL) AS DISPONIVEL FROM AD_VW_ESTOQUELOCAL  WHERE CODPROD = :P_CODPROD  AND CODEMP IN (:P_CODEMP, 0)  AND CODLOCAL = :P_CODLOCAL ");

		NativeSql queryEstoqueTerceiros = new NativeSql(jdbc);

		queryEstoqueTerceiros.setNamedParameter("P_CODPROD", codprod);

		queryEstoqueTerceiros.setNamedParameter("P_CODEMP", codemp);

		queryEstoqueTerceiros.setNamedParameter("P_CODLOCAL", codlocalorig);

		ResultSet rsEstoqueTerceiros = queryEstoqueTerceiros.executeQuery(
				"SELECT SUM(ESTOQUE - RESERVADO) AS ESTOQUETERC  FROM TGFEST EST  WHERE EST.CODPROD = :P_CODPROD  AND EST.CODEMP = :P_CODEMP  AND EST.CODLOCAL = :P_CODLOCAL  AND CODPARC IN (SELECT CODPARC FROM AD_TERCCORTEGLOBAL) ");

		BigDecimal estoqueTerceiros = BigDecimal.ZERO;

		if (rsEstoqueTerceiros.next()) {

			estoqueTerceiros = (rsEstoqueTerceiros.getBigDecimal("ESTOQUETERC") == null) ? BigDecimal.ZERO : rsEstoqueTerceiros.getBigDecimal("ESTOQUETERC");

		}

		if (rsEstLocal.next()) {

			disponivel = rsEstLocal.getBigDecimal("DISPONIVEL").add(qtdneg).subtract(estoqueTerceiros);

		}

		if (qtdLiberar.compareTo(disponivel) > 0) {

			return "N";

		}

		return "S";

	}

	public static boolean isMultiplo(BigDecimal valor, BigDecimal multiplo) {

		BigDecimal resto = valor.remainder(multiplo);

		return (resto.compareTo(BigDecimal.ZERO) == 0);

	}

	private static void openSession() {

		try {

			if (isOpen.booleanValue() && jdbc != null) {

				return;

			}

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

			hnd = JapeSession.open();

			hnd.setFindersMaxRows(-1);

			hnd.setCanTimeout(false);

			jdbc = dwfFacade.getJdbcWrapper();

			jdbc.openSession();

			isOpen = Boolean.valueOf(true);

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	private static void closeSession() {

		if (isOpen.booleanValue()) {

			JdbcWrapper.closeSession(jdbc);

			JapeSession.close(hnd);

			isOpen = Boolean.valueOf(false);

			jdbc = null;

		}

	}

}