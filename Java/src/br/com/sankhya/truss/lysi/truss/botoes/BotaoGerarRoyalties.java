package br.com.lysi.truss.botoes;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;

import org.jdom.Element;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.XMLUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.EntityPrimaryKey;
import br.com.sankhya.jape.dao.EntityPropertyDescriptor;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.AtributosRegras;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.BarramentoRegra.DadosBarramento;
import br.com.sankhya.modelcore.comercial.CentralFinanceiro;
import br.com.sankhya.modelcore.comercial.ClientEvent;
import br.com.sankhya.modelcore.comercial.LiberacaoSolicitada;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.comercial.util.LoteAutomaticoHelper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.ListenerParameters;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class BotaoGerarRoyalties implements AcaoRotinaJava {
	private static final SimpleDateFormat	ddMMyyyySkw	= new SimpleDateFormat("dd/MM/yyyy");

	public void doAction(ContextoAcao contexto) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		if (contexto.getLinhas().length == 0) {
			contexto.mostraErro("Selecione uma nota antes.");
		} else if (contexto.getLinhas().length > 1) {
			contexto.mostraErro("Selecione apenas uma nota de cada vez.");
		}

		contexto.confirmar("Geração Royalties", "Esta opção vai gerar uma nota Royalty/Taxa de publicidade.<br>Deseja continuar?", 1);

		Registro linha = contexto.getLinhas()[0];
		
		PersistentLocalEntity notaPersistent = dwfEntityFacade.findEntityByPrimaryKey(DynamicEntityNames.CABECALHO_NOTA, linha.getCampo("NUNOTA"));
		DynamicVO notaOrigemVO = (DynamicVO) notaPersistent.getValueObject();
		
		
		DynamicVO parceiroVO = notaOrigemVO.asDymamicVO(DynamicEntityNames.PARCEIRO);

		if ("1".equals(notaOrigemVO.asString("AD_STATUSROYAL"))) {
			contexto.mostraErro("Royaltes já gerados, não será possível continuar.");
		}

		if (!"L".equals(notaOrigemVO.asString("STATUSNOTA"))) {
			contexto.mostraErro("Para gerar os royaltes, a nota deve estar confirmada.");
		}
		
		BigDecimal codEmpRoyalties = parceiroVO.asBigDecimal("AD_EMPROYALTIES");
		if (codEmpRoyalties == null)
			contexto.mostraErro(String.format("Não configurado a Empresa de Royalties para o parceiro %s-%s.", parceiroVO.asBigDecimal("CODPARC"), parceiroVO.asString("NOMEPARC")));
		
		BigDecimal codEmpTaxa = parceiroVO.asBigDecimal("AD_EMPTAXA");
		if (codEmpTaxa == null)
			contexto.mostraErro(String.format("Não configurado a Empresa de Taxa Publicidade para o parceiro %s-%s.", parceiroVO.asBigDecimal("CODPARC"), parceiroVO.asString("NOMEPARC")));

		Collection<DynamicVO> configuracoes = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper("AD_CONFIGROYALTIESSERV", "this.CODEMP = ? AND this.CODEMPDEST IN ( ?, ? )"
				, new Object[] { notaOrigemVO.asBigDecimal("CODEMP"), codEmpRoyalties, codEmpTaxa }));

		if (configuracoes.size() == 0) {
			contexto.mostraErro(String.format("Não foi localizada na tela 'Configuração Royalties' a configuração para a empresa %s-%s.", notaOrigemVO.asBigDecimal("CODEMP"), notaOrigemVO.asString("Empresa.RAZAOSOCIAL")));
		}

		if (parceiroVO.asBigDecimal("AD_CODTABSERV") == null) {
			contexto.mostraErro(String.format("Não foi localizada 'Tabela de preço para serviços' na tela 'Parceiros' para o parceiro %s-%s.", parceiroVO.asBigDecimal("CODPARC"), parceiroVO.asString("NOMEPARC")));
		}

		boolean booCalcularRoyaltiesPorParceiro = "S".equals(MGECoreParameter.getParameterAsString("CALCROYPARC"));
		
		BigDecimal vlrTotal = null;
		
		if ( booCalcularRoyaltiesPorParceiro )
			vlrTotal = NativeSql.getBigDecimal("SUM(VLRTOT - VLRDESC) * ? ", "TGFITE", "USOPROD != 'D' AND NUNOTA = ? ", new Object[] { parceiroVO.asBigDecimal("AD_PERCSERVICOS"), notaOrigemVO.asBigDecimal("NUNOTA") });    
		else
			vlrTotal = NativeSql.getBigDecimal("SUM(QTDNEG * SNK_PRECO(?, CODPROD))", "TGFITE", "USOPROD != 'D' AND NUNOTA = ? AND NVL(AD_DUZIA, 'N') = 'N'", new Object[] { parceiroVO.asBigDecimal("AD_CODTABSERV"), notaOrigemVO.asBigDecimal("NUNOTA") });
		
		if ( vlrTotal == null )
			contexto.mostraErro("Nota não tem itens que atendam a regra de geração de royalties.");
			
		vlrTotal = BigDecimalUtil.getRounded( vlrTotal , 2 );
		
		CACHelper cacHelper = new CACHelper();
		JapeSessionContext.putProperty(ListenerParameters.CENTRAIS, Boolean.TRUE);
		StringBuffer strNotas = new StringBuffer();
		BigDecimal totalPerc = BigDecimal.ZERO;

		for (DynamicVO configVO : configuracoes) {
			totalPerc = totalPerc.add(configVO.asBigDecimalOrZero("PERCTAXA"));
		}

		if (totalPerc.doubleValue() != 100) {
			contexto.mostraErro("Os percentuais configurados na tela 'Configuração Royalties' estão diferente de 100%. Percentual total: " + BigDecimalUtil.toCurrency(totalPerc));
		}

		for (DynamicVO configVO : configuracoes) {
			Collection<DynamicVO> tops = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper(DynamicEntityNames.TIPO_OPERACAO, "this.CODTIPOPER = ?", new Object[] { configVO.asBigDecimal("CODTIPOPER") }));
			DynamicVO topVO = tops.iterator().next();

			Element elemCabecalho = new Element("Cabecalho");
			XMLUtils.addContentElement(elemCabecalho, "NUNOTA", "");
			XMLUtils.addContentElement(elemCabecalho, "NUMNOTA", BigDecimal.ZERO);
			XMLUtils.addContentElement(elemCabecalho, "CODEMP", configVO.asBigDecimal("CODEMPDEST"));
			XMLUtils.addContentElement(elemCabecalho, "CODEMPNEGOC", configVO.asBigDecimal("CODEMPDEST"));
			XMLUtils.addContentElement(elemCabecalho, "CODPARC", parceiroVO.asBigDecimal("CODPARC"));
			XMLUtils.addContentElement(elemCabecalho, "CODTIPOPER", topVO.asBigDecimal("CODTIPOPER"));
			XMLUtils.addContentElement(elemCabecalho, "TIPMOV", topVO.asString("TIPMOV"));
			XMLUtils.addContentElement(elemCabecalho, "CODTIPVENDA", notaOrigemVO.asBigDecimal("CODTIPVENDA"));
			XMLUtils.addContentElement(elemCabecalho, "DTNEG", ddMMyyyySkw.format(notaOrigemVO.asTimestamp("DTNEG")));
			XMLUtils.addContentElement(elemCabecalho, "CODCENCUS", configVO.asBigDecimal("CODCENCUS"));
			XMLUtils.addContentElement(elemCabecalho, "CODNAT", configVO.asBigDecimal("CODNAT"));
			XMLUtils.addContentElement(elemCabecalho, "CIF_FOB", "S");//Sem Frete
			XMLUtils.addContentElement(elemCabecalho, "AD_NUNOTA", notaOrigemVO.asBigDecimal("NUNOTA"));
			XMLUtils.addContentElement(elemCabecalho, "AD_CODUSUROY", contexto.getUsuarioLogado());

			BarramentoRegra barra = cacHelper.incluirAlterarCabecalho(ServiceContext.getCurrent(), elemCabecalho);

			EntityPropertyDescriptor[] fds = barra.getState().getDao().getSQLProvider().getPkObjectUID().getFieldDescriptors();

			Collection<EntityPrimaryKey> pksEnvolvidas = barra.getDadosBarramento().getPksEnvolvidas();
			EntityPrimaryKey cabKey = pksEnvolvidas.iterator().next();
			BigDecimal nuNota = null;

			for (int i = 0; i < fds.length; i++) {
				EntityPropertyDescriptor cabEntity = fds[i];

				if ("NUNOTA".equals(cabEntity.getField().getName())) {
					nuNota = new BigDecimal(cabKey.getValues()[i].toString());
				}
			}

			if (nuNota == null) {
				throw new Exception("Não foi possível gerar a nota");
			}

			dwfEntityFacade.clearSessionCache("CabecalhoNota");

			cacHelper.addIncluirAlterarListener(new LoteAutomaticoHelper());
			JapeSession.putProperty(AtributosRegras.INC_UPD_ITEM_CENTRAL, Boolean.TRUE);

			Element itensElem = new Element("itens");
			itensElem.setAttribute("ATUALIZACAO_ONLINE", "false");

			if (strNotas.length() > 0) {
				strNotas.append(", ");
			}

			strNotas.append(nuNota);

			DynamicVO prodItemVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO(DynamicEntityNames.SERVICO, new Object[] { configVO.asBigDecimal("CODPROD") });
			BigDecimal valorUnitario = BigDecimalUtil.getRounded(vlrTotal.multiply(configVO.asBigDecimalOrZero("PERCTAXA")).divide(BigDecimalUtil.CEM_VALUE, BigDecimalUtil.MATH_CTX), prodItemVO.asInt("DECVLR"));

			Element itemElem = new Element("item");
			XMLUtils.addContentElement(itemElem, "NUNOTA", nuNota);
			XMLUtils.addContentElement(itemElem, "SEQUENCIA", "");
			XMLUtils.addContentElement(itemElem, "CODPROD", prodItemVO.asBigDecimal("CODPROD"));
			XMLUtils.addContentElement(itemElem, "CODVOL", prodItemVO.asString("CODVOL"));
			XMLUtils.addContentElement(itemElem, "QTDNEG", BigDecimal.ONE);
			XMLUtils.addContentElement(itemElem, "PERCDESC", BigDecimal.ZERO);
			XMLUtils.addContentElement(itemElem, "VLRDESC", BigDecimal.ZERO);
			XMLUtils.addContentElement(itemElem, "VLRUNIT", valorUnitario);
			XMLUtils.addContentElement(itemElem, "VLRTOT", valorUnitario);

			itensElem.addContent(itemElem);

			DadosBarramento dadosBarramento = cacHelper.incluirAlterarItem(nuNota, ServiceContext.getCurrent(), itensElem, false);

			EntityPropertyDescriptor[] fdsItem = dwfEntityFacade.getDAOInstance("ItemNota").getSQLProvider().getPkObjectUID().getFieldDescriptors();
			Collection<EntityPrimaryKey> pksEnvolvidasItem = dadosBarramento.getPksEnvolvidas();
			BigDecimal sequencia = null;

			if (pksEnvolvidasItem != null && pksEnvolvidasItem.size() > 0) {
				EntityPrimaryKey itemKey = pksEnvolvidasItem.iterator().next();

				for (int i = 0; i < fdsItem.length; i++) {
					EntityPropertyDescriptor itemEntity = fdsItem[i];

					if ("SEQUENCIA".equals(itemEntity.getField().getName())) {
						sequencia = new BigDecimal(itemKey.getValues()[i].toString());
					}
				}
			}
			
			gravarDescontoCredito( parceiroVO.asBigDecimal("CODPARC"), configVO.asBigDecimal("CODEMPDEST"), nuNota  );

			Collection<Exception> erros = dadosBarramento.getErros();

			if (erros.size() > 0) {
				throw erros.iterator().next();
			}

			Collection<LiberacaoSolicitada> liberacoes = dadosBarramento.getLiberacoesSolicitadas();

			if (liberacoes.size() > 0) {
				throw new Exception("Não foi possível gerar a nota. Solicitação de liberação gerada: " + liberacoes.iterator().next().getDescricao());
			}

			Collection<ClientEvent> clientEvents = dadosBarramento.getClientEvents();

			if (clientEvents.size() > 0) {
				throw new Exception("Não foi possível gerar a nota. Evento solicitado: " + clientEvents.iterator().next().getEventID());
			}

			if (sequencia == null) {
				throw new Exception("Não foi possível gerar a nota. Nenhum item gerado.");
			}
		}

		notaOrigemVO.setProperty("AD_STATUSROYAL", "1");
		notaPersistent.setValueObject((EntityVO) notaOrigemVO);

		contexto.setMensagemRetorno(String.format("Pedidos %s gerados com sucesso.", strNotas.toString()));
	}
	
	
	private void gravarDescontoCredito(BigDecimal codParc, BigDecimal codEmp, BigDecimal nuNota ) throws Exception {
		JdbcWrapper	jdbc;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbc = dwfEntityFacade.getJdbcWrapper();
		NativeSql 	nativeSql 	= null;
		try {
			jdbc.openSession();
			nativeSql = new NativeSql(jdbc);
			
			BigDecimal percUsoPermitido = new BigDecimal(MGECoreParameter.getParameterAsInt("PERCPDUSACRED"));
			if ( BigDecimalUtil.isEmpty(percUsoPermitido))
				throw new Exception("Parâmetro PERCPDUSACRED não está configurado.");

			//verificando se tem credito a compensar
			String strWhere = String.format("CODPARC = %s AND CODEMP = %s", codParc, codEmp );
			
			BigDecimal vlrCredito = NativeSql.getBigDecimal( "SUM( NVL(VLRCREDITO,0) - NVL(( SELECT SUM(VLRCONSUMO) FROM AD_CREDITOCONSUMO CO WHERE CO.NUCREDITO = CR.NUCREDITO ),0) )"
															, "AD_CREDITOCLIENTE CR"
															, strWhere );
			if ( BigDecimalUtil.isEmpty(vlrCredito) )
				return;
			
			//buscando o pedido
			JapeWrapper pedidoDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
			DynamicVO  pedidoVO = pedidoDAO.findByPK(nuNota);
			if(pedidoVO == null)
				return;
			
			
			//calculando limite de uso do crédito
			BigDecimal limiteCreditoPD = pedidoVO.asBigDecimal("VLRNOTA").multiply(percUsoPermitido).divide(BigDecimalUtil.CEM_VALUE).setScale(2, RoundingMode.HALF_UP);
			if ( vlrCredito.compareTo(limiteCreditoPD) > 0 )
				vlrCredito = limiteCreditoPD;
			
			//atualizando desconto no rodapé do pedido
			gravarDescontoItens( pedidoVO, vlrCredito );
			
			NumberFormat formatter = NumberFormat.getCurrencyInstance();
			
			String strObservacao = String.format( "Valor original do pedido %s\n"
								 				+ "Valor desconto aplicado  %s\n"
								 				+ "Valor liquido            %s" 
								 				,  formatter.format(pedidoVO.asBigDecimal("VLRNOTA"))
								 				,  formatter.format(vlrCredito)
								 				,  formatter.format(pedidoVO.asBigDecimal("VLRNOTA").subtract(vlrCredito))
								 				);
			
			
			DynamicVO NEWpedidoVO = pedidoDAO.findByPK(nuNota);
			CentralFinanceiro centralFinanceiro = new CentralFinanceiro();
			centralFinanceiro.excluiFinanceiro(nuNota);
			
			ImpostosHelpper	impostosHelper = new ImpostosHelpper();
			impostosHelper.setForcarRecalculo(true);
			impostosHelper.forcaRecalculoBaseISS(true);
			impostosHelper.recalculoICMS(pedidoVO, pedidoVO);
			
			pedidoDAO.prepareToUpdate(pedidoVO)
				.set("OBSERVACAO", strObservacao)
				.set("AD_VLRDESCPGTOAVISTA", vlrCredito)
				.update();
			
			
			//amarrando o credito consumido ao pedido
			gravarConsumoCredito( pedidoVO, vlrCredito );
			
		} finally {
			JdbcWrapper.closeSession(jdbc);
			NativeSql.releaseResources(nativeSql);
		}
	}
	
	
	private void gravarDescontoItens( DynamicVO pedidoVO, BigDecimal vlrCredito )  throws Exception {
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
		jdbc.openSession();
		try {	
				
				
				Collection<DynamicVO> itens = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper(DynamicEntityNames.ITEM_NOTA, "this.NUNOTA = ? ", new Object[] { pedidoVO.asBigDecimal("NUNOTA") }));
				if (itens.isEmpty() )
					return;
				
				if ( pedidoVO.asBigDecimal("VLRNOTA").compareTo(BigDecimal.ZERO) == 0 )
					return;
				
				BigDecimal indiceDescCredito = vlrCredito.divide(pedidoVO.asBigDecimal("VLRNOTA"), MathContext.DECIMAL128).setScale( 6, RoundingMode.HALF_UP);
				
				JapeWrapper itemDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
				
				BigDecimal descontoAplicado = BigDecimal.ZERO;
				int itemNro = 0;
				
				for ( DynamicVO itemVO : itens ) {
					
					//calculando valores
					BigDecimal vlrDesconto = itemVO.asBigDecimal("VLRUNIT").multiply(indiceDescCredito).setScale(2, RoundingMode.HALF_UP);
					BigDecimal vlrUnit = itemVO.asBigDecimal("VLRUNIT").subtract(vlrDesconto);
					descontoAplicado = descontoAplicado.add(vlrDesconto);
					
					itemNro++;
					//se for o último item, aplica o resto
					if ( itemNro == itens.size()  ) {
						BigDecimal resto = vlrCredito.subtract(descontoAplicado);
						if ( resto.compareTo(BigDecimal.ZERO) > 0 )
							vlrUnit = vlrUnit.add( resto );
					}
					
					BigDecimal vlrTot  = vlrUnit.multiply(itemVO.asBigDecimal("QTDNEG")).setScale(2, RoundingMode.HALF_UP);
						
					//atualizando item
					itemDAO.prepareToUpdate(itemVO)
						.set("VLRUNIT", vlrUnit )
						.set("VLRTOT",  vlrTot )
						.update();
					
				}
				
		} catch (Exception e) {
			throw new Exception("Erro gravando desconto itens.\n\n" + e.getMessage(), e);
		} finally {
			JdbcWrapper.closeSession(jdbc);
		}
	}
	
	private void gravarConsumoCredito( DynamicVO pedidoVO, BigDecimal totalConsumido )  throws Exception {
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
		jdbc.openSession();
		try {	
			// BUSCANDO CREDITO A CONSUMIR
			StringBuffer queryCredito =  new StringBuffer(NativeSql.loadSQLFromResource(this.getClass(), "creditoConsumir.sql"));
			NativeSql nSql = new NativeSql(jdbc);
			nSql.appendSql(queryCredito.toString());
			nSql.setNamedParameter("CODPARC", pedidoVO.asBigDecimal("CODPARC") );
			nSql.setNamedParameter("CODEMP",  pedidoVO.asBigDecimal("CODEMP") );
			ResultSet rs = nSql.executeQuery();
			NativeSql insertSQL = new NativeSql(jdbc);
			BigDecimal vlrConsumo;
			while ( rs.next() ) {

				//verifica quem é maior, o consumo ou o crédito
				if ( totalConsumido.compareTo( rs.getBigDecimal("VLRCREDITO") ) < 0 )
				  vlrConsumo = totalConsumido;
				else
				  vlrConsumo = rs.getBigDecimal("VLRCREDITO");
					  
					
				//insere o consumo do crédito
				insertSQL.executeUpdate( String.format(" INSERT INTO AD_CREDITOCONSUMO (NUCREDITO, NUNOTA, VLRCONSUMO) VALUES ( %s, %s, %s )" 
														, rs.getBigDecimal("NUCREDITO")
														, pedidoVO.asBigDecimal("NUNOTA") 
														, vlrConsumo
														) );
				//retira o que foi consumido
				totalConsumido = totalConsumido.subtract( vlrConsumo );

				//se zerou o consumo, finaliza
				if ( totalConsumido.compareTo(BigDecimal.ZERO) == 0 )
					break;
			}
			rs.close();
				
		} catch (Exception e) {
			throw new Exception("Erro gravando consumo do crédito.\n\n" + e.getMessage(), e);
		} finally {
			JdbcWrapper.closeSession(jdbc);
		}
	}
}
