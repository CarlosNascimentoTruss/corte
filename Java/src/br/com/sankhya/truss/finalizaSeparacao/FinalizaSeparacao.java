package br.com.sankhya.truss.finalizaSeparacao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.SessionContext;

import org.jdom.Element;

import com.google.gson.JsonObject;
import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.JsonUtils;
import com.sankhya.util.StringUtils;
import com.sankhya.util.TimeUtils;
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
import br.com.sankhya.jape.vo.PrePersistEntityState;
import br.com.sankhya.jape.vo.VOProperty;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.AtributosRegras;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.BarramentoRegra.DadosBarramento;
import br.com.sankhya.modelcore.comercial.CentralFaturamento;
import br.com.sankhya.modelcore.comercial.CentralItemNota;
import br.com.sankhya.modelcore.comercial.ConfirmacaoNotaHelper;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.helper.ConferenciaHelper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.ListenerParameters;
import br.com.sankhya.modelcore.util.SPBeanUtils;
import br.com.sankhya.ws.ServiceContext;


public class FinalizaSeparacao implements AcaoRotinaJava {
	private static final SimpleDateFormat	ddMMyyyySkw			= new SimpleDateFormat("dd/MM/yyyy");
	private static final SimpleDateFormat	dhFormat			= new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	private static final long				serialVersionUID	= 1L;
	protected SessionContext				context;
	
	
	
	
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		try {
		ServiceContext sctx = ServiceContext.getCurrent();
		
		Registro[] linhas = ctx.getLinhas();
		
		for(Registro linha : linhas) {
			
			BigDecimal qtdVolumes = new BigDecimal((Integer) ctx.getParam("P_QTDVOLUMES"));
			String especie = (String) ctx.getParam("P_ESPECIE");
			BigDecimal numConferencia = (BigDecimal) linha.getCampo("AD_NUMCONFERENCIA");
			
			
			
			finalizarConferencia(sctx, qtdVolumes, especie, numConferencia);
			
			
		}
		
		} catch(Exception e) {
			ctx.mostraErro(e.getMessage());
		}
		
	}
	
	
	public void finalizarConferencia(ServiceContext ctx, BigDecimal qtdVolumes, String especie, BigDecimal numConferencia) throws Exception {
		JapeSession.SessionHandle hnd = null;
		JdbcWrapper jdbc = null;

		try {
			hnd = JapeSession.open();
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbc = dwfEntityFacade.getJdbcWrapper();
			jdbc.openSession();

			JsonObject requestBody = ctx.getJsonRequestBody();
			//BigDecimal numConferencia = JsonUtils.getBigDecimal(requestBody, "numConf");
			//BigDecimal qtdVolumes = JsonUtils.getBigDecimal(requestBody, "qtdVolumes");
			//String especie = JsonUtils.getString(requestBody, "especie");

			PersistentLocalEntity conferenciaEntity = dwfEntityFacade.findEntityByPrimaryKey("AD_TRASCONF", new Object[] { numConferencia });
			DynamicVO conferenciaVO = (DynamicVO) conferenciaEntity.getValueObject();
			BigDecimal nuNota = conferenciaVO.asBigDecimal("NUNOTA");

			if ("F".equals(conferenciaVO.asString("STATUS"))) {
				throw new Exception("No  possvel alterar esta conferncia, pois a conferncia j esta finalizada.");
			}

			NativeSql sql = new NativeSql(jdbc);
			sql.loadSql(this.getClass(), "ConferenciaExpedicaoSPBean_quantidadeConferida.sql");
			sql.setNamedParameter("NUNOTA", nuNota);
			sql.setNamedParameter("NUCONF", numConferencia);

			ResultSet rs = sql.executeQuery();
			String status = ConferenciaHelper.STATUS_FINALIZADA_OK;

			while (rs.next()) {
				if (rs.getDouble("QTDPEDIDO") != rs.getDouble("QTDCONFERIDA")) {
					throw new Exception(String.format("No ser possvel finalizar a conferncia pois o produto %s no est totalmente conferido.", rs.getBigDecimal(1)));
				}
			}
			rs.close();

			JapeSessionContext.putProperty(ListenerParameters.CENTRAIS, Boolean.TRUE);

			conferenciaVO.setProperty("STATUS", status);
			conferenciaVO.setProperty("DHFINCONF", TimeUtils.getNow());
			conferenciaVO.setProperty("QTDVOL", qtdVolumes);
			conferenciaVO.setProperty("VOLUME", especie);
			conferenciaVO.setProperty("CODUSUFINCONF", ((AuthenticationInfo) ctx.getAutentication()).getUserID());
			conferenciaEntity.setValueObject((EntityVO) conferenciaVO);

			sql = new NativeSql(jdbc);
			sql.appendSql(" SELECT CODPROD, ");
			sql.appendSql("   CONTROLE, ");
			sql.appendSql("   SUM(QTDUNITPADRAO) AS QTDCONF ");
			sql.appendSql(" FROM AD_TRASITEMCONF ");
			sql.appendSql(" WHERE NUCONF = :NUCONF ");
			sql.appendSql(" GROUP BY CODPROD, CONTROLE ");
			sql.appendSql(" ORDER BY CODPROD, CONTROLE ");
			sql.setNamedParameter("NUCONF", numConferencia);

			rs = sql.executeQuery();
			CACHelper cacHelper = new CACHelper();
			Map<String, Map<String, Object>> mapQtds = new HashMap<String, Map<String, Object>>();

			while (rs.next()) {
				BigDecimal codProd = rs.getBigDecimal("CODPROD");
				String controle = rs.getString("CONTROLE");
				BigDecimal qtdConferida = rs.getBigDecimal("QTDCONF");

				String chave = codProd + "-" + controle;
				Map<String, Object> dados = new HashMap<String, Object>();
				dados.put("CODPROD", codProd);
				dados.put("CONTROLE", controle);
				dados.put("QTDCONF", qtdConferida);
				mapQtds.put(chave, dados);
			}
			rs.close();

			for (Entry<String, Map<String, Object>> entry : mapQtds.entrySet()) {
				Map<String, Object> dados = entry.getValue();
				BigDecimal codProd = (BigDecimal) dados.get("CODPROD");
				String controle = (String) dados.get("CONTROLE");
				BigDecimal qtdConferida = (BigDecimal) dados.get("QTDCONF");

				Collection<DynamicVO> itens = null;

				do {
					FinderWrapper finder = new FinderWrapper(DynamicEntityNames.ITEM_NOTA, "this.NUNOTA = ? AND this.CODPROD = ? AND this.AD_LOTE IS NULL", new Object[] { nuNota, codProd });
					finder.setOrderBy("CODPROD, USOPROD DESC, SEQUENCIA");
					itens = dwfEntityFacade.findByDynamicFinderAsVO(finder);

					if (itens.size() > 0) {
						DynamicVO itemVO = itens.iterator().next();

						if (itemVO.asDouble("QTDNEG") > qtdConferida.doubleValue()) {
							itemVO.setAceptTransientProperties(true);
							BigDecimal sequencia = itemVO.asBigDecimal("SEQUENCIA");
							BigDecimal vlrUnitario = itemVO.asBigDecimal("VLRUNIT");

							//desativamos a validao de estoque
							//Precisamos fazer isto porque a rotina esta validando de maneira incorreta o estoque, sendo que no modificamos a quantidade e mesmo assim a rotina no permite falando que esta sem estoque
							DynamicVO produtoVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO(DynamicEntityNames.PRODUTO, itemVO.asBigDecimal("CODPROD"));
							PersistentLocalEntity persistentGrupo = dwfEntityFacade.findEntityByPrimaryKey(DynamicEntityNames.GRUPO_PRODUTO, produtoVO.asBigDecimal("CODGRUPOPROD"));
							DynamicVO grupoVO = (DynamicVO) persistentGrupo.getValueObject();
							String oldValidacao = grupoVO.asString("VALEST");
							grupoVO.setProperty("VALEST", "N");
							persistentGrupo.setValueObject((EntityVO) grupoVO);

							BigDecimal qtdNegRestante = itemVO.asBigDecimal("QTDNEG").subtract(qtdConferida);
							BigDecimal oldQtdNeg = itemVO.asBigDecimal("QTDNEG");
							BigDecimal oldVlrIPI = itemVO.asBigDecimalOrZero("AD_VLRIPI");
							BigDecimal oldVlrSUBST = itemVO.asBigDecimalOrZero("AD_VLRSUBST");
							BigDecimal vlrIPI = BigDecimalUtil.getRounded(qtdConferida.multiply(oldVlrIPI).divide(oldQtdNeg, BigDecimalUtil.MATH_CTX), 2);
							BigDecimal vlrSUBST = BigDecimalUtil.getRounded(qtdConferida.multiply(oldVlrSUBST).divide(oldQtdNeg, BigDecimalUtil.MATH_CTX), 2);

							itemVO.setProperty("QTDNEG", qtdConferida);
							itemVO.setProperty("CONTROLE", controle);
							itemVO.setProperty("AD_LOTE", controle);
							itemVO.setProperty("VLRTOTMOE", BigDecimalUtil.getRounded(itemVO.asBigDecimalOrZero("VLRUNITMOE").multiply(itemVO.asBigDecimalOrZero("QTDNEG")), 2));
							itemVO.setProperty("AD_VLRIPI", vlrIPI);
							itemVO.setProperty("AD_VLRSUBST", vlrSUBST);
							//itemVO.setProperty("CODLOCALORIG", configVO.asBigDecimal("CODLOCALDEST"));

							CentralItemNota centralItemNota = new CentralItemNota();
							//centralItemNota.recalcularValores("QTDNEG", oldQtdNeg.toString(), itemVO, nuNota);
							itemVO.setProperty("BASEICMS", null);
							
							
							
							
							
							ArrayList<DynamicVO> itensToUpdate = new ArrayList<DynamicVO>();
							itensToUpdate.add(itemVO);
							DadosBarramento dadosBarramento = cacHelper.incluirAlterarItem(nuNota, ServiceContext.getCurrent(), null, false, itensToUpdate);
							
							Collection<Exception> erros = dadosBarramento.getErros();
							
							if (erros.size() > 0) {
								throw new Exception("No foi possvel atualizar o Lote do pedido. Erro solicitado: " + erros.iterator().next());
							}

							//atualizamos o VO para obter modificaes que possam ocorrer no banco
							itemVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO(DynamicEntityNames.ITEM_NOTA, new Object[] { nuNota, sequencia });

							if (itemVO.asDouble("VLRUNIT") != vlrUnitario.doubleValue()) {
								//quando altera a quantidade do item esta recalculado perdendo o valor de desconto aplicado ao valor unitrio, ento para ajustar isto vamos fazer o update do vlr unitrio
								itemVO.setProperty("VLRUNIT", vlrUnitario);
								itemVO.setProperty("VLRTOT", qtdConferida.multiply(vlrUnitario));

								centralItemNota.recalcularValores("VLRUNIT", "0", itemVO, nuNota);
								centralItemNota.recalcularValores("VLRTOT", "0", itemVO, nuNota);
								
								itensToUpdate = new ArrayList<DynamicVO>();
								itensToUpdate.add(itemVO);
								cacHelper.incluirAlterarItem(nuNota, ServiceContext.getCurrent(), null, false, itensToUpdate);
							}

							//inclui um novo item
							itemVO.setProperty("SEQUENCIA", null);
							itemVO.setProperty("QTDNEG", qtdNegRestante);
							itemVO.setProperty("AD_LOTE", null);
							itemVO.setProperty("CONTROLE", " ");
							itemVO.setProperty("VLRTOTMOE", BigDecimalUtil.getRounded(itemVO.asBigDecimalOrZero("VLRUNITMOE").multiply(itemVO.asBigDecimalOrZero("QTDNEG")), 2));
							itemVO.setProperty("AD_VLRIPI", BigDecimalUtil.getRounded(oldVlrIPI.subtract(vlrIPI), 2));
							itemVO.setProperty("AD_VLRSUBST", BigDecimalUtil.getRounded(oldVlrSUBST.subtract(vlrSUBST), 2));
							//itemVO.setProperty("CODLOCALORIG", configVO.asBigDecimal("CODLOCALDEST"));

							//centralItemNota.recalcularValores("QTDNEG", "0", itemVO, nuNota);
							itemVO.setProperty("BASEICMS", null);

							BigDecimal sequenciaOrigemFormula = null;

							if ("D".equals(itemVO.asString("USOPROD"))) {
								Collection<DynamicVO> ligacoes = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper(DynamicEntityNames.COMPRA_VENDA_VARIOS_PEDIDO, "this.NUNOTA = ? AND this.NUNOTAORIG = ? AND this.SEQUENCIA = ?", new Object[] { nuNota, nuNota, sequencia }));

								if (ligacoes.size() > 0) {
									DynamicVO varVO = ligacoes.iterator().next();
									sequenciaOrigemFormula = varVO.asBigDecimal("SEQUENCIAORIG");

									DynamicVO itemPaiVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO(DynamicEntityNames.ITEM_NOTA, new Object[] { nuNota, sequencia });

									itemVO.setProperty("CODPRODPAIMP", itemPaiVO.asBigDecimal("CODPROD"));
									itemVO.setProperty("SEQPRODPAI", sequenciaOrigemFormula);
								}
								itemVO.setProperty("FORCEUSOPROD", "D");
							}

							dadosBarramento = cacHelper.incluirAlterarItem(nuNota, ServiceContext.getCurrent(), buildItemNota(itemVO), false);
							Collection<EntityPrimaryKey> pksEnvolvidasItem = dadosBarramento.getPksEnvolvidas();
							EntityPropertyDescriptor[] fdsItem = dwfEntityFacade.getDAOInstance("ItemNota").getSQLProvider().getPkObjectUID().getFieldDescriptors();
							BigDecimal novaSequencia = null;

							if (pksEnvolvidasItem != null && pksEnvolvidasItem.size() > 0) {
								EntityPrimaryKey itemKey = pksEnvolvidasItem.iterator().next();

								for (int i = 0; i < fdsItem.length; i++) {
									EntityPropertyDescriptor itemEntity = fdsItem[i];

									if ("SEQUENCIA".equals(itemEntity.getField().getName())) {
										novaSequencia = new BigDecimal(itemKey.getValues()[i].toString());
									}
								}
							}

							DynamicVO novoItemVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO(DynamicEntityNames.ITEM_NOTA, new Object[] { nuNota, novaSequencia });

							if ("D".equals(itemVO.asString("USOPROD"))) {
								//Mesmo o item possuindo o USOPROD = D, a rotina volta ele para V porque o produto tambm pode ser vendido fora da formula, ento verificamos e caso no seja D, foramos o update.
								if (!"D".equals(novoItemVO.asString("USOPROD"))) {
									novoItemVO.setProperty("USOPROD", "D");
									novoItemVO.setProperty("CONTROLE", controle);

									itensToUpdate = new ArrayList<DynamicVO>();
									itensToUpdate.add(novoItemVO);
									dadosBarramento = cacHelper.incluirAlterarItem(nuNota, ServiceContext.getCurrent(), null, false, itensToUpdate);

									erros = dadosBarramento.getErros();

									if (erros.size() > 0) {
										throw new Exception("No foi possvel atualizar o Usoprod do pedido. Erro solicitado: " + erros.iterator().next());
									}
								}
								
								if (novaSequencia != null && sequenciaOrigemFormula != null) {
									Collection<DynamicVO> ligacoes = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper(DynamicEntityNames.COMPRA_VENDA_VARIOS_PEDIDO, "this.NUNOTA = ? AND this.NUNOTAORIG = ? AND this.SEQUENCIA = ?", new Object[] { nuNota, nuNota, novaSequencia }));

									if (ligacoes.size() == 0) {
										DynamicVO variosPedidosVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance(DynamicEntityNames.COMPRA_VENDA_VARIOS_PEDIDO);
										variosPedidosVO.setProperty("NUNOTA", nuNota);
										variosPedidosVO.setProperty("SEQUENCIA", novaSequencia);
										variosPedidosVO.setProperty("NUNOTAORIG", nuNota);
										variosPedidosVO.setProperty("SEQUENCIAORIG", sequenciaOrigemFormula);
										variosPedidosVO.setProperty("STATUSNOTA", novoItemVO.asString("STATUSNOTA"));
										dwfEntityFacade.createEntity(DynamicEntityNames.COMPRA_VENDA_VARIOS_PEDIDO, (EntityVO) variosPedidosVO);
									}
								}
							}

							//quando duplica o item esta recalculado perdendo o valor de desconto aplicado ao valor unitrio, ento para ajustar isto vamos fazer o update do vlr unitrio
							//quando duplica o item esta recalculado perdendo o valor de desconto aplicado ao valor unitrio, ento para ajustar isto vamos fazer o update do vlr unitrio
							if (novoItemVO.asDouble("VLRUNIT") != vlrUnitario.doubleValue()) {
								novoItemVO.setProperty("VLRUNIT", vlrUnitario);

								centralItemNota.recalcularValores("VLRUNIT", "0", novoItemVO, nuNota);

								itensToUpdate = new ArrayList<DynamicVO>();
								itensToUpdate.add(novoItemVO);
								cacHelper.incluirAlterarItem(nuNota, ServiceContext.getCurrent(), null, false, itensToUpdate);
							}

							//voltamos a validao de estoque do grupo conforme estava antes
							persistentGrupo = dwfEntityFacade.findEntityByPrimaryKey(DynamicEntityNames.GRUPO_PRODUTO, produtoVO.asBigDecimal("CODGRUPOPROD"));
							grupoVO = (DynamicVO) persistentGrupo.getValueObject();
							grupoVO.setProperty("VALEST", oldValidacao);
							persistentGrupo.setValueObject((EntityVO) grupoVO);

							erros = dadosBarramento.getErros();

							if (erros.size() > 0) {
								throw new Exception("No foi possvel criar o novo item do pedido. Erro solicitado: " + erros.iterator().next());
							}

							qtdConferida = BigDecimal.ZERO;
						} else {
							itemVO.setProperty("CONTROLE", controle);
							itemVO.setProperty("AD_LOTE", controle);
							//itemVO.setProperty("CODLOCALORIG", configVO.asBigDecimal("CODLOCALDEST"));

							CentralItemNota centralItemNota = new CentralItemNota();
							centralItemNota.recalcularValores("CONTROLE", " ", itemVO, nuNota);

							ArrayList<DynamicVO> itensToUpdate = new ArrayList<DynamicVO>();
							itensToUpdate.add(itemVO);
							DadosBarramento dadosBarramento = cacHelper.incluirAlterarItem(nuNota, ServiceContext.getCurrent(), null, false, itensToUpdate);

							Collection<Exception> erros = dadosBarramento.getErros();

							if (erros.size() > 0) {
								throw new Exception("No foi possvel atualizar o Lote do pedido. Erro solicitado: " + erros.iterator().next());
							}

							qtdConferida = qtdConferida.subtract(itemVO.asBigDecimal("QTDNEG"));
						}

					}
				} while (itens.size() > 0 && qtdConferida.intValue() > 0);
			}
			rs.close();

			DynamicVO cabVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO(DynamicEntityNames.CABECALHO_NOTA, nuNota);

			NativeSql sqlNota = new NativeSql(jdbc);
			sqlNota.appendSql("UPDATE TGFCAB  ");
			sqlNota.appendSql("  SET VOLUME = :VOLUME, ");
			sqlNota.appendSql("      QTDVOL = :QTDVOL ");

			if (cabVO.asInt("AD_SEQAGRUP") == 0) {
				sqlNota.appendSql("      , AD_STATUSPED = '6' ");
			}

			sqlNota.appendSql(" WHERE NUNOTA = :NUNOTA");
			sqlNota.setNamedParameter("VOLUME", especie);
			sqlNota.setNamedParameter("QTDVOL", qtdVolumes);
			sqlNota.setNamedParameter("NUNOTA", nuNota);
			sqlNota.executeUpdate();

			if (cabVO.asInt("AD_SEQAGRUP") > 0) {
				NativeSql sqlCountFinalizados = new NativeSql(jdbc);
				sqlCountFinalizados.appendSql(" SELECT COUNT(1) ");
				sqlCountFinalizados.appendSql(" FROM TGFCAB CAB, AD_TRASCONF CONF ");
				sqlCountFinalizados.appendSql(" WHERE CAB.AD_NUMCONFERENCIA = CONF.NUCONF");
				sqlCountFinalizados.appendSql("   AND CAB.AD_SEQAGRUP = :SEQAGRUP");
				sqlCountFinalizados.appendSql("   AND CONF.STATUS = 'F'");
				sqlCountFinalizados.setNamedParameter("SEQAGRUP", cabVO.asBigDecimalOrZero("AD_SEQAGRUP"));

				ResultSet rsCountFinalizados = sqlCountFinalizados.executeQuery();
				BigDecimal qtdNotasFinalizadosConferencia = BigDecimal.ZERO;

				if (rsCountFinalizados.next()) {
					qtdNotasFinalizadosConferencia = BigDecimalUtil.getValueOrZero(rsCountFinalizados.getBigDecimal(1));
				}
				rsCountFinalizados.close();

				NativeSql sqlCountNotasGrupo = new NativeSql(jdbc);
				sqlCountNotasGrupo.appendSql(" SELECT COUNT(1) ");
				sqlCountNotasGrupo.appendSql(" FROM TGFCAB CAB ");
				sqlCountNotasGrupo.appendSql(" WHERE CAB.AD_SEQAGRUP = :SEQAGRUP");
				sqlCountNotasGrupo.setNamedParameter("SEQAGRUP", cabVO.asBigDecimalOrZero("AD_SEQAGRUP"));

				ResultSet rsCountNotasGrupo = sqlCountNotasGrupo.executeQuery();
				BigDecimal qtdCountNotasGrupo = BigDecimal.ZERO;

				if (rsCountNotasGrupo.next()) {
					qtdCountNotasGrupo = BigDecimalUtil.getValueOrZero(rsCountNotasGrupo.getBigDecimal(1));
				}
				rsCountNotasGrupo.close();

				if (qtdCountNotasGrupo.intValue() == qtdNotasFinalizadosConferencia.intValue()) {
					NativeSql sqlAgrupamento = new NativeSql(jdbc);
					sqlAgrupamento.appendSql("UPDATE TGFCAB SET AD_STATUSPED = '6' WHERE AD_SEQAGRUP = :SEQAGRUP");
					sqlAgrupamento.setNamedParameter("SEQAGRUP", cabVO.asBigDecimalOrZero("AD_SEQAGRUP"));
					sqlAgrupamento.executeUpdate();
				}
			}

			Collection<DynamicVO> configs = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper("AD_CONFIGEXPEDICAO", "this.CODIGO = ?", new Object[] { cabVO.asBigDecimal("CODEMP") }));

			if (configs.size() > 0) {
				DynamicVO configVO = configs.iterator().next();

				if ("S".equals(configVO.asString("GERARTRANSF"))) {
					BigDecimal topEstoque = null;
					Timestamp dhTipOper = null;

					//atualizamos a cabVO
					PersistentLocalEntity persistentCab = dwfEntityFacade.findEntityByPrimaryKey(DynamicEntityNames.CABECALHO_NOTA, new Object[] { nuNota });
					cabVO = (DynamicVO) persistentCab.getValueObject();

					Collection<DynamicVO> tops = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper(DynamicEntityNames.TIPO_OPERACAO, "this.CODTIPOPER = ? ", new Object[] { cabVO.asBigDecimal("CODTIPOPER") }));
					DynamicVO topVO = tops.iterator().next();

					if (topVO.asBigDecimal("AD_TOPCONFEXP") != null) {
						topEstoque = cabVO.asBigDecimal("CODTIPOPER");
						dhTipOper = cabVO.asTimestamp("DHTIPOPER");

						tops = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper(DynamicEntityNames.TIPO_OPERACAO, "this.CODTIPOPER = ? ", new Object[] { topVO.asBigDecimal("AD_TOPCONFEXP") }));
						topVO = tops.iterator().next();

						NativeSql sqlUpdate = new NativeSql(jdbc);
						sqlUpdate.appendSql(" UPDATE TGFCAB ");
						sqlUpdate.appendSql("   SET CODTIPOPER = ?,");
						sqlUpdate.appendSql("       DHTIPOPER = ?");
						sqlUpdate.appendSql(" WHERE NUNOTA = ?");
						sqlUpdate.addParameter(topVO.asBigDecimal("CODTIPOPER"));
						sqlUpdate.addParameter(topVO.asTimestamp("DHALTER"));
						sqlUpdate.addParameter(nuNota);
						sqlUpdate.executeUpdate();

						NativeSql sqlItem = new NativeSql(jdbc);
						sqlItem.appendSql(" UPDATE TGFITE ");
						sqlItem.appendSql("   SET ATUALESTOQUE = :ATUALESTOQUE,");
						sqlItem.appendSql("       RESERVA = :RESERVA");
						sqlItem.appendSql(" WHERE NUNOTA = :NUNOTA");

						if ("B".equals(topVO.asString("ATUALEST"))) {
							sqlItem.setNamedParameter("ATUALESTOQUE", BigDecimal.valueOf(-1));
						} else if ("R".equals(topVO.asString("ATUALEST")) || "E".equals(topVO.asString("ATUALEST"))) {
							sqlItem.setNamedParameter("ATUALESTOQUE", BigDecimal.valueOf(1));
						} else {
							sqlItem.setNamedParameter("ATUALESTOQUE", BigDecimal.valueOf(0));
						}

						if ("R".equals(topVO.asString("ATUALEST"))) {
							sqlItem.setNamedParameter("RESERVA", "S");
						} else {
							sqlItem.setNamedParameter("RESERVA", "N");
						}

						sqlItem.setNamedParameter("NUNOTA", nuNota);
						sqlItem.executeUpdate();
					}

					BigDecimal novoNuNota = inserirTransferenciaInterna(dwfEntityFacade, nuNota, configVO);

					conferenciaEntity = dwfEntityFacade.findEntityByPrimaryKey("AD_TRASCONF", new Object[] { numConferencia });
					conferenciaVO = (DynamicVO) conferenciaEntity.getValueObject();
					conferenciaVO.setProperty("NUNOTATRANSF", novoNuNota);
					conferenciaEntity.setValueObject((EntityVO) conferenciaVO);

					FinderWrapper finder = new FinderWrapper(DynamicEntityNames.ITEM_NOTA, "this.NUNOTA = ?", new Object[] { nuNota });
					finder.setOrderBy("SEQUENCIA");
					finder.setMaxResults(-1);
					Collection<DynamicVO> itens = dwfEntityFacade.findByDynamicFinderAsVO(finder);
					ArrayList<DynamicVO> itensToUpdate = new ArrayList<DynamicVO>();

					for (DynamicVO itemVO : itens) {
						BigDecimal codLocalOrigem = itemVO.asBigDecimal("CODLOCALORIG");
						itemVO.setProperty("CODLOCALORIG", configVO.asBigDecimal("CODLOCALDEST"));

						CentralItemNota centralItemNota = new CentralItemNota();
						centralItemNota.recalcularValores("CODLOCALORIG", StringUtils.getNullAsEmpty(codLocalOrigem), itemVO, nuNota);

						itensToUpdate.add(itemVO);
					}
					DadosBarramento dadosBarramentoItem = cacHelper.incluirAlterarItem(nuNota, ServiceContext.getCurrent(), null, false, itensToUpdate);

					Collection<Exception> errosItem = dadosBarramentoItem.getErros();

					if (errosItem.size() > 0) {
						throw new Exception("No foi possvel atualizar o Lote do pedido. Erro solicitado: " + errosItem.iterator().next());
					}

					if (topEstoque != null) {
						//atualizamos a cabVO
						NativeSql sqlUpdate = new NativeSql(jdbc);
						sqlUpdate.appendSql(" UPDATE TGFCAB ");
						sqlUpdate.appendSql("   SET CODTIPOPER = ?,");
						sqlUpdate.appendSql("       DHTIPOPER = ?");
						sqlUpdate.appendSql(" WHERE NUNOTA = ?");
						sqlUpdate.addParameter(topEstoque);
						sqlUpdate.addParameter(dhTipOper);
						sqlUpdate.addParameter(nuNota);
						sqlUpdate.executeUpdate();

						NativeSql sqlItem = new NativeSql(jdbc);
						sqlItem.appendSql(" UPDATE TGFITE ");
						sqlItem.appendSql("   SET ATUALESTOQUE = :ATUALESTOQUE,");
						sqlItem.appendSql("       RESERVA = :RESERVA");
						sqlItem.appendSql(" WHERE NUNOTA = :NUNOTA");

						topVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO(DynamicEntityNames.TIPO_OPERACAO, new Object[] { topEstoque, dhTipOper });

						if ("B".equals(topVO.asString("ATUALEST"))) {
							sqlItem.setNamedParameter("ATUALESTOQUE", BigDecimal.valueOf(-1));
						} else if ("R".equals(topVO.asString("ATUALEST")) || "E".equals(topVO.asString("ATUALEST"))) {
							sqlItem.setNamedParameter("ATUALESTOQUE", BigDecimal.valueOf(1));
						} else {
							sqlItem.setNamedParameter("ATUALESTOQUE", BigDecimal.valueOf(0));
						}

						if ("R".equals(topVO.asString("ATUALEST"))) {
							sqlItem.setNamedParameter("RESERVA", "S");
						} else {
							sqlItem.setNamedParameter("RESERVA", "N");
						}

						sqlItem.setNamedParameter("NUNOTA", nuNota);
						sqlItem.executeUpdate();
					}

					NativeSql sqlUpdate = new NativeSql(jdbc);
					sqlUpdate.appendSql("UPDATE TGFCAB SET AD_PEDORIGEM = :PEDORIGEM WHERE NUNOTA = :NUNOTA");
					sqlUpdate.setNamedParameter("PEDORIGEM", novoNuNota);
					sqlUpdate.setNamedParameter("NUNOTA", nuNota);
					sqlUpdate.executeUpdate();
				}
			}

		} catch (Exception e) {
			throw new Exception(e.getMessage());
		} finally {
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
		}
	}
	
	private Element buildItemNota(DynamicVO itemVO) {
		Element itensElem = new Element("itens");
		itensElem.setAttribute("ATUALIZACAO_ONLINE", "false");

		Element itemElem = new Element("item");

		for (Iterator ite = itemVO.iterator(); ite.hasNext();) {
			VOProperty property = (VOProperty) ite.next();

			try {
				if (property.getValue() == null) {
					XMLUtils.addContentElement(itemElem, property.getName(), property.getValue());

				} else if (!(property.getValue() instanceof Timestamp)) {
					XMLUtils.addContentElement(itemElem, property.getName(), property.getValue());
				}
			} catch (Exception ignored) {
			}
		}
		itensElem.addContent(itemElem);
		return itensElem;
	}
	
	private BigDecimal inserirTransferenciaInterna(EntityFacade dwfEntityFacade, BigDecimal nuNota, DynamicVO configVO) throws Exception {
		DynamicVO cabVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO(DynamicEntityNames.CABECALHO_NOTA, nuNota);

		FinderWrapper finder = new FinderWrapper(DynamicEntityNames.ITEM_NOTA, "this.NUNOTA = ?", new Object[] { nuNota });
		finder.setOrderBy("SEQUENCIA");
		finder.setMaxResults(-1);
		Collection<DynamicVO> itens = dwfEntityFacade.findByDynamicFinderAsVO(finder);

		Collection<DynamicVO> tops = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper(DynamicEntityNames.TIPO_OPERACAO, "this.CODTIPOPER = ? ", new Object[] { cabVO.asBigDecimal("CODTIPOPER") }));
		DynamicVO topVO = tops.iterator().next();

		if (topVO.asBigDecimal("AD_TOPDESTINO") == null) {
			throw new Exception("No foi configurado a TOP de Destino para a transferncia.");
		}

		tops = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper(DynamicEntityNames.TIPO_OPERACAO, "this.CODTIPOPER = ? ", new Object[] { topVO.asBigDecimal("AD_TOPDESTINO") }));
		topVO = tops.iterator().next();

		Element elemCabecalho = new Element("Cabecalho");
		XMLUtils.addContentElement(elemCabecalho, "NUNOTA", "");
		XMLUtils.addContentElement(elemCabecalho, "CODEMP", configVO.asBigDecimal("CODEMP"));
		XMLUtils.addContentElement(elemCabecalho, "CODPARC", configVO.asBigDecimal("CODPARC"));
		XMLUtils.addContentElement(elemCabecalho, "CODTIPOPER", topVO.asBigDecimal("CODTIPOPER"));

		//if ("T".equals(topVO.asString("TIPMOV"))) {
		XMLUtils.addContentElement(elemCabecalho, "CODTIPVENDA", BigDecimal.ZERO);
		/*} else {
			Collection<DynamicVO> negociacoes = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper(DynamicEntityNames.TIPO_NEGOCIACAO, "this.CODTIPVENDA = ? ", new Object[] { configVO.asBigDecimalOrZero("CODTIPVENDA") }));
			DynamicVO negociacaoVO = negociacoes.iterator().next();

			XMLUtils.addContentElement(elemCabecalho, "CODTIPVENDA", negociacaoVO.asBigDecimal("CODTIPVENDA"));
			XMLUtils.addContentElement(elemCabecalho, "DHTIPVENDA", dhFormat.format(negociacaoVO.asTimestamp("DHALTER")));
		}*/
		XMLUtils.addContentElement(elemCabecalho, "DTNEG", ddMMyyyySkw.format(TimeUtils.getNow()));
		XMLUtils.addContentElement(elemCabecalho, "TIPMOV", topVO.asString("TIPMOV"));
		XMLUtils.addContentElement(elemCabecalho, "CODCENCUS", configVO.asBigDecimal("CODCENCUS"));
		XMLUtils.addContentElement(elemCabecalho, "CODNAT", configVO.asBigDecimal("CODNAT"));
		XMLUtils.addContentElement(elemCabecalho, "CIF_FOB", "S");//Sem Frete
		XMLUtils.addContentElement(elemCabecalho, "AD_PEDORIGEM", nuNota);

		CACHelper cacHelper = new CACHelper();
		BarramentoRegra barra = cacHelper.incluirAlterarCabecalho(ServiceContext.getCurrent(), elemCabecalho);

		EntityPropertyDescriptor[] fds = barra.getState().getDao().getSQLProvider().getPkObjectUID().getFieldDescriptors();

		Collection<EntityPrimaryKey> pksEnvolvidas = barra.getDadosBarramento().getPksEnvolvidas();
		EntityPrimaryKey cabKey = pksEnvolvidas.iterator().next();

		for (int i = 0; i < fds.length; i++) {
			EntityPropertyDescriptor cabEntity = fds[i];

			if ("NUNOTA".equals(cabEntity.getField().getName())) {
				nuNota = new BigDecimal(cabKey.getValues()[i].toString());
			}
		}

		JapeSession.putProperty(AtributosRegras.INC_UPD_ITEM_CENTRAL, Boolean.TRUE);

		Element itensElem = new Element("itens");
		itensElem.setAttribute("ATUALIZACAO_ONLINE", "false");

		for (DynamicVO itemVO : itens) {
			BigDecimal codProd = itemVO.asBigDecimal("CODPROD");
			BigDecimal codLocalOrig = itemVO.asBigDecimal("CODLOCALORIG");
			BigDecimal quantidade = itemVO.asBigDecimal("QTDNEG");
			String controle = itemVO.asString("CONTROLE");

			DynamicVO prodVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO(DynamicEntityNames.PRODUTO, codProd);

			Element itemElem = new Element("item");
			XMLUtils.addContentElement(itemElem, "SEQUENCIA", "");
			XMLUtils.addContentElement(itemElem, "NUNOTA", nuNota);
			XMLUtils.addContentElement(itemElem, "CODPROD", prodVO.asBigDecimal("CODPROD"));
			XMLUtils.addContentElement(itemElem, "CODVOL", prodVO.asString("CODVOL"));
			XMLUtils.addContentElement(itemElem, "QTDNEG", quantidade);
			XMLUtils.addContentElement(itemElem, "CONTROLE", controle);
			XMLUtils.addContentElement(itemElem, "PERCDESC", "0");
			XMLUtils.addContentElement(itemElem, "VLRDESC", "0");
			XMLUtils.addContentElement(itemElem, "CODLOCALORIG", codLocalOrig);
			XMLUtils.addContentElement(itemElem, "CODLOCALDEST", configVO.asBigDecimal("CODLOCALDEST"));

			itensElem.addContent(itemElem);
		}

		DadosBarramento dadosBarramento = cacHelper.incluirAlterarItem(nuNota, ServiceContext.getCurrent(), itensElem, true);

		Collection<Exception> erros = dadosBarramento.getErros();

		if (erros.size() > 0) {
			throw erros.iterator().next();
		}

		BarramentoRegra barramentoConfirmacao = BarramentoRegra.build(CentralFaturamento.class, "regrasConfirmacaoSilenciosa.xml", AuthenticationInfo.getCurrent());
		barramentoConfirmacao.setValidarSilencioso(true);

		PrePersistEntityState persistentConf = ConfirmacaoNotaHelper.confirmarNota(nuNota, barramentoConfirmacao, true);
		DynamicVO notaVO = persistentConf.getNewVO();

		if (!"L".equals(notaVO.asString("STATUSNOTA"))) {
			StringBuffer strErros = new StringBuffer();

			if (!barramentoConfirmacao.getErros().isEmpty()) {
				for (Exception e : barramentoConfirmacao.getErros()) {
					if (strErros.toString().length() > 0) {
						strErros.append("\n");
					}
					strErros.append(e.getMessage());
				}
			}

			if (strErros.length() > 0) {
				throw new Exception(String.format("Falha ao confirmar transferncia: %s", strErros.toString()));
			}
		}

		return nuNota;
	}

	




}
