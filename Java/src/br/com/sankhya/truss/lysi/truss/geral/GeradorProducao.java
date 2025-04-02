package br.com.sankhya.truss.lysi.truss.geral;

import java.math.BigDecimal;
import java.sql.ResultSet;

import org.omg.IOP.ServiceContext;

import com.sankhya.util.StringUtils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.mgeprod.model.geradorIMPS.LancamentoOPBuilder;
import br.com.sankhya.mgeprod.model.geradorIMPS.ProdutoGeradorLancamento;
import br.com.sankhya.mgeprod.model.lancamento.LancamentoOP;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class GeradorProducao {
	
	private SessionHandle hnd = null;
	private JdbcWrapper	jdbc = null;
	private NativeSql	nativeSql = null;
	private ServiceContext	sctx;
	private BigDecimal nulop = null;
	private LancamentoOPBuilder lancOPBuilder = null;
	private String topsGerarProducaoKit = "";
	
	public Boolean gerarProducao() throws Exception {	
		
		Boolean booGerou = false;
		
		hnd = null;
		jdbc = null;
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			hnd = JapeSession.open();
			jdbc = dwfEntityFacade.getJdbcWrapper();
			jdbc.openSession();
			
			nativeSql = new NativeSql(jdbc);
			
			try {
				topsGerarProducaoKit = MGECoreParameter.getParameterAsString("TOPOPKIT").replaceAll(" ","").replaceAll("\r", ",").replaceAll("\n", ",").replaceAll(";", ",").replaceAll(",,", ",");
				
				if (StringUtils.getNullAsEmpty(topsGerarProducaoKit).isEmpty())
					throw new Exception("Par�metro TOPOPKIT n�o configurado.");
				
				//buscando quantidade a produzir por produto
				String sql = "  SELECT I.CODPROD, SUM( QTDNEG ) AS QTDNEG "
							+ "	FROM TGFITE I, TGFCAB C, TGFPRO P "
							+ " WHERE C.DTNEG BETWEEN TRUNC(SYSDATE)-1 AND SYSDATE "
							+ "   AND C.NUNOTA = I.NUNOTA "
							+ "   AND C.TIPMOV = 'P' "
							+ "   AND C.CODTIPOPER IN ( " + topsGerarProducaoKit + " ) "
							+ "   AND C.PENDENTE = 'S' "
							+ "   AND I.CODPROD = P.CODPROD "
							+ "   AND P.AD_OPAUTOMATICA = 'S' "
							+ "   AND NOT EXISTS ( SELECT 1 FROM AD_OP_ITENS O WHERE O.NUNOTA = I.NUNOTA AND O.SEQUENCIA = I.SEQUENCIA ) "
							+ "   AND I.CONTROLE = ' '"
							+ " GROUP BY I.CODPROD ";
				
				
				
				ResultSet rs = nativeSql.executeQuery(sql);
				while(rs.next()) {
					
					nulop = getNovoLancamentoOP( getProduto( rs.getBigDecimal("CODPROD") ) );
					lancOPBuilder  = LancamentoOPBuilder.build(jdbc, nulop);
					
					BigDecimal seqProducao = gerarProdutoOP( rs.getBigDecimal("QTDNEG"), rs.getBigDecimal("CODPROD") );
					lancOPBuilder.persistir();
					
					vincularItensPDsComOP( rs.getBigDecimal("CODPROD"), nulop );
					
					//gravar numero dos pedidos na observa��o da OP, como?
					booGerou = true;
				}
				
				if (booGerou)
					System.out.println("*** GERADOR PRODUCAO - Produ��o gerada com sucesso ***");
				else
					System.out.println("*** GERADOR PRODUCAO - N�o h� pedidos para gerar produ��o ***");
					
			} catch (Exception e) {
				System.out.println("*** GERADOR PRODUCAO - ERRO ***");
				e.printStackTrace();
			}
		} finally {
			JdbcWrapper.closeSession(jdbc);
			NativeSql.releaseResources(nativeSql);
			JapeSession.close(hnd);
		}
		return booGerou;
	}
	
	private String getProduto( BigDecimal codProd ) throws Exception {
		JapeWrapper produtoDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
		DynamicVO produtoVO = produtoDAO.findByPK(codProd);
		String produto = produtoVO.asBigDecimal("CODPROD").toString() + " - " + produtoVO.asString("DESCRPROD"); 
		return produto.substring(0,35);
	}
	
	private BigDecimal gerarProdutoOP( BigDecimal tamLote, BigDecimal codProd ) throws Exception{
		
		ProdutoGeradorLancamento produtoLancamento = null;
		BigDecimal codParc = null;
		BigDecimal nunota = null;
		BigDecimal sequencia = null;
		String     controle = "0000";
		
		BigDecimal codPlp = new BigDecimal(2);
		return lancOPBuilder.inserirOPProducao(codProd, controle, codPlp, nunota, tamLote, sequencia, codParc, produtoLancamento);
	}
	
	private void vincularItensPDsComOP( BigDecimal codprod, BigDecimal  seqProducao ) throws Exception {
		
		// buscando itens com o produto
		String sqlItens = "  SELECT DISTINCT I.NUNOTA, I.SEQUENCIA "
				+ "	FROM TGFITE I, TGFCAB C, TGFPRO P "
				+ " WHERE C.DTNEG BETWEEN TRUNC(SYSDATE)-1 AND SYSDATE "
				+ "   AND C.NUNOTA = I.NUNOTA "
				+ "   AND C.TIPMOV = 'P' "
				+ "   AND C.CODTIPOPER IN ( " + topsGerarProducaoKit + " ) "
				+ "   AND C.PENDENTE = 'S' "
				+ "   AND I.CODPROD = P.CODPROD "
				+ "   AND P.AD_OPAUTOMATICA = 'S' "
				+ "   AND NOT EXISTS ( SELECT 1 FROM AD_OP_ITENS O WHERE O.NUNOTA = I.NUNOTA AND O.SEQUENCIA = I.SEQUENCIA ) "
				+ "   AND I.CONTROLE = ' '"
				+ "   AND I.CODPROD = " + codprod;
		
		nativeSql.setReuseStatements(false);
		nativeSql.resetSqlBuf();
		ResultSet rsItens = nativeSql.executeQuery(sqlItens);
		
		
		//atualizando itens com nro da OP
		String updItens = " INSERT INTO AD_OP_ITENS ( NUNOTA, SEQUENCIA, NULOP ) VALUES ( :NUNOTA, :SEQUENCIA, :NULOP ) ";
		
		nativeSql.resetSqlBuf();
		nativeSql.appendSql(updItens);
		nativeSql.setReuseStatements(true);
		nativeSql.setBatchUpdateSize(50);
		
		while(rsItens.next()) {
			nativeSql.setNamedParameter("NUNOTA", 		rsItens.getBigDecimal("NUNOTA"));
			nativeSql.setNamedParameter("SEQUENCIA", 	rsItens.getBigDecimal("SEQUENCIA"));
			nativeSql.setNamedParameter("NULOP", 		seqProducao );
			nativeSql.addBatch();
		}
		nativeSql.flushBatchTail();
	
	}
	
	
	private BigDecimal getNovoLancamentoOP( String produto ) throws Exception {
			
			String descricao = "Produ��o KIT: " + produto;
			boolean reutilizar = false;

			LancamentoOP lancamentoOPBean = new LancamentoOP(jdbc);
			lancamentoOPBean.setDescricao(descricao);
			lancamentoOPBean.setReutilizar(reutilizar);
			return lancamentoOPBean.persistir();

	}

}
