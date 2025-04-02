package br.com.sankhya.truss.validaComposicao;


import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class validaServicoComposicaoPa implements EventoProgramavelJava {

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {

		String acao = "U";
		validaServicoComposicao(arg0, acao);
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
		String acao = "D";
		validaServicoComposicao(arg0, acao);

	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		String acao = "I";
		validaServicoComposicao(arg0, acao);

	}

	public static void validaServicoComposicao(PersistenceEvent arg0, String acao) throws Exception {
		System.out.println("validaServicoComposicaoPa.atualizaCampoProduto()");

		DynamicVO newVO = (DynamicVO) arg0.getVo();

		BigDecimal idefx = BigDecimalUtil.getValueOrZero((BigDecimal) newVO.getProperty("IDEFX"));
		BigDecimal codprodPa = BigDecimalUtil.getValueOrZero((BigDecimal) newVO.getProperty("CODPRODPA"));
		BigDecimal codprodMp = BigDecimalUtil.getValueOrZero((BigDecimal) newVO.getProperty("CODPRODMP"));

		JapeWrapper tpratvDAO = JapeFactory.dao(DynamicEntityNames.ATIVIDADE);
		DynamicVO tpratvVO = tpratvDAO.findOne(" IDEFX = " + idefx);

		JapeWrapper tprprcDAO = JapeFactory.dao(DynamicEntityNames.PROCESSO_PRODUTIVO);
		DynamicVO tprprcVO = tprprcDAO.findOne(" IDPROC = " + tpratvVO.getProperty("IDPROC"));
		
		
		BigDecimal codPlp = tprprcVO.asBigDecimal("CODPLP");
		BigDecimal plantaPadrao = new BigDecimal(3);

		if (codPlp.compareTo(plantaPadrao) == 0) {

			EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();

			DynamicVO parametroVO = (DynamicVO) entityFacade.findEntityByPrimaryKeyAsVO("ParametroSistema",
					new Object[] { "GRUPOSERVTERC", BigDecimal.ZERO });
			int grupo = parametroVO.asInt("INTEIRO");

			@SuppressWarnings("unused")
			boolean continua = true;

			if (grupo == 0) {
				continua = false;
				throw new MGEModelException("Necesssário cadastrar o parâmetro GRUPOSERVTERC.");
			}

			if (acao.equals("I") || acao.equals("U")) {

				JapeWrapper tprlmpDAO = JapeFactory.dao(DynamicEntityNames.LISTA_MATERIAIS_ATIVIDADE);
				Collection<DynamicVO> tprlmpVO = tprlmpDAO
						.find(" this.IDEFX = " + idefx + " AND this.CODPRODPA = " + codprodPa
								+ " AND EXISTS (SELECT 1 FROM TGFPRO WHERE CODPROD = this.CODPRODMP AND CODGRUPOPROD = "
								+ grupo + " )");

				JapeWrapper tgfproDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
				DynamicVO tgfproVO = tgfproDAO.findOne(" CODPROD = " + codprodMp + " AND CODGRUPOPROD = " + grupo);

				if (tgfproVO == null) {

					if (tprlmpVO.isEmpty()) {
						exibirMensagem();

					}
				}

			}

			if (acao.equals("D")) {

				JapeWrapper tprlmpDAO = JapeFactory.dao(DynamicEntityNames.LISTA_MATERIAIS_ATIVIDADE);
				Collection<DynamicVO> tprlmpVO = tprlmpDAO
						.find(" this.IDEFX = " + idefx + " AND this.CODPRODPA = " + codprodPa
								+ " AND EXISTS (SELECT 1 FROM TGFPRO WHERE CODPROD = this.CODPRODMP AND CODGRUPOPROD = "
								+ grupo + " ) AND CODPRODMP <> " + codprodMp);

				if (tprlmpVO.isEmpty()) {
					exibirMensagem2();
				}

			}

		}
	}

	public static void exibirMensagem() throws IOException {
		throw new IOException(
				"Só é permitido inserir ou alterar um registro na composição da planta 3 se já houver um serviço de industrialização já cadastrado.");
	}

	public static void exibirMensagem2() throws IOException {
		throw new IOException(
				"Só é permitido excluir um serviço da composição da planta 3 se já houver um outro serviço de industrialização já cadastrado.");
	}

}
