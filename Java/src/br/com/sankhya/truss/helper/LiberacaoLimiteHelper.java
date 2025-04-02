package br.com.sankhya.dctm.helper;

import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

public class LiberacaoLimiteHelper {

	public FluidCreateVO criaLiberacaoLimite(BigDecimal nuchave, String tabela, BigDecimal evento, BigDecimal sequencia,
			BigDecimal seqcascata, BigDecimal nucll, BigDecimal codususolicit, Timestamp dhsolicit,
			BigDecimal vlrlimite, BigDecimal vlratual, String observacao, BigDecimal perclimite, BigDecimal vlrtotal,
			BigDecimal codcencus, BigDecimal codtipoper, BigDecimal ordem, BigDecimal codusulib) throws Exception {

		JapeWrapper jw = JapeFactory.dao(DynamicEntityNames.LIBERACAO_LIMITE);
		DynamicVO lib = jw.findByPK(new Object[] { nuchave, tabela, evento, sequencia, seqcascata, nucll });
		if (lib == null) {

			FluidCreateVO createLiberacao = JapeFactory.dao(DynamicEntityNames.LIBERACAO_LIMITE).create();

			createLiberacao.set("NUCHAVE", nuchave);
			createLiberacao.set("TABELA", tabela);
			createLiberacao.set("EVENTO", evento);
			createLiberacao.set("SEQUENCIA", sequencia);
			createLiberacao.set("SEQCASCATA", seqcascata);
			createLiberacao.set("NUCLL", nucll);

			createLiberacao.set("CODUSUSOLICIT", codususolicit);
			createLiberacao.set("DHSOLICIT", dhsolicit);
			createLiberacao.set("OBSERVACAO", observacao);
			createLiberacao.set("VLRLIMITE", vlrlimite);
			createLiberacao.set("VLRATUAL", vlratual);
			createLiberacao.set("PERCLIMITE", perclimite);
			createLiberacao.set("VLRTOTAL", vlrtotal);
			createLiberacao.set("CODCENCUS", codcencus);
			createLiberacao.set("CODTIPOPER", codtipoper);
			createLiberacao.set("ORDEM", ordem);
			createLiberacao.set("CODUSULIB", codusulib);

			return createLiberacao;
		} else {
			throw new MGEModelException("Já existe uma Liberação de Limite com esta chave primária.");
		}
	}

	public FluidCreateVO insereCampoLiberacaoLimite(FluidCreateVO fo, String campo, Object valor) {
		fo.set(campo, valor);
		return fo;
	}

	public void salvaLiberacaoLimite(FluidCreateVO fo) throws Exception {
		fo.save();
	}
}
