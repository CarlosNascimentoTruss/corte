package br.com.sankhya.truss.royalties;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;

import org.jdom.Content;
import org.jdom.Element;

import com.sankhya.util.BigDecimalUtil;
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
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.CentralFinanceiro;
import br.com.sankhya.modelcore.comercial.ClientEvent;
import br.com.sankhya.modelcore.comercial.ContextoRegra;
import br.com.sankhya.modelcore.comercial.LiberacaoSolicitada;
import br.com.sankhya.modelcore.comercial.Regra;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.comercial.util.LoteAutomaticoHelper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class AplicaRoyalties implements Regra, AcaoRotinaJava  {
	String erroAoCalcular = "N";
	
	@Override
	public void afterDelete(ContextoRegra arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(ContextoRegra arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(ContextoRegra arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(ContextoRegra arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(ContextoRegra arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(ContextoRegra arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
		Registro linha = ctx.getLinhas()[0];
		
		DynamicVO cabVO = cabDAO.findByPK((BigDecimal) linha.getCampo("NUNOTA"));
		aplicaRoyalties(cabVO);
	}
	
	private void aplicaRoyalties(DynamicVO cabVO) throws Exception {
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JapeWrapper configRoyaltiesDAO = JapeFactory.dao("AD_CONFIGROYALTIESSERV");
		JapeWrapper topDAO = JapeFactory.dao(DynamicEntityNames.TIPO_OPERACAO);
		JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
		SimpleDateFormat ddMMyyyySkw = new SimpleDateFormat("dd/MM/yyyy");
		BigDecimal usuarioLogado = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
		
		
		BigDecimal nunota = cabVO.asBigDecimal("NUNOTA");
		RoyaltiesHelper royaltiesHelper = new RoyaltiesHelper();
		
		PersistentLocalEntity notaPersistent = dwfEntityFacade.findEntityByPrimaryKey("CabecalhoNota", nunota);
		
		// Retorna se a TOP não estiver parametrizada para gerar royalties
		if(!royaltiesHelper.topGeraRoyalties(cabVO.asBigDecimal("CODTIPOPER"))) {
			return;
		}
		
		
		JapeWrapper parDAO = JapeFactory.dao(DynamicEntityNames.PARCEIRO);
		DynamicVO parVO = parDAO.findByPK(cabVO.asBigDecimal("CODPARC"));
		
		// Verifica cadastro da empresa de royalties no parceiro
		BigDecimal codEmpRoyalties = parVO.asBigDecimal("AD_EMPROYALTIES");
		if(codEmpRoyalties == null) {
			throw new Exception("Não configurado a Empresa de Royalties para o parceiro");
		}
		
		// Verifica cadastro de empresa de taxa de publicidade no parceiro
		BigDecimal codEmpTaxa = parVO.asBigDecimal("AD_EMPTAXA");
		if(codEmpTaxa == null) {
			throw new Exception("Não configurado a Empresa de Taxa Publicidade para o parceiro");
		}
		
		// Verifica cadastro de configuração de royalties para a empresa
		Collection<DynamicVO> configsRoyaltiesVO = configRoyaltiesDAO.find("CODEMP = ? AND CODEMPDEST IN ( ?, ? )",  cabVO.asBigDecimal("CODEMP"), codEmpRoyalties, codEmpTaxa); 
		if(configsRoyaltiesVO.size() == 0) {
			throw new Exception("Não foi localizada na tela 'Configuração Royalties' a configuração para a empresa " + cabVO.asBigDecimal("CODEMP"));
		}
		
		//Verifica tabela de preço de serviços no parceiro
		BigDecimal tabPrecoServ = parVO.asBigDecimal("AD_CODTABSERV");
		if(tabPrecoServ == null) {
			throw new Exception("Não foi localizada 'Tabela de prepara serviços' na tela 'Parceiros' para o parceiro");
		}
		
		
		boolean booCalcularRoyaltiesPorParceiro = "S".equals(MGECoreParameter.getParameterAsString("CALCROYPARC"));
		BigDecimal vlrTotal = null;
		
		
		if (booCalcularRoyaltiesPorParceiro) {
			vlrTotal = NativeSql.getBigDecimal("SUM(VLRTOT - VLRDESC) * ? ", "TGFITE", "USOPROD != 'D' AND NUNOTA = ? ",  new Object[] { tabPrecoServ, nunota });
		} else {
			vlrTotal = NativeSql.getBigDecimal("SUM(QTDNEG * SNK_PRECO(?, CODPROD))", "TGFITE", "USOPROD != 'D' AND NUNOTA = ? AND NVL(AD_DUZIA, 'N') = 'N'",  new Object[] { tabPrecoServ, nunota });
		} 
		
		vlrTotal = BigDecimalUtil.getRounded(vlrTotal, 2);
		
		CACHelper cacHelper = new CACHelper();
		JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
		StringBuffer strNotas = new StringBuffer();
		BigDecimal totalPerc = BigDecimal.ZERO;
		for (DynamicVO configVO : configsRoyaltiesVO) {
		   totalPerc = totalPerc.add(configVO.asBigDecimalOrZero("PERCTAXA")); 
		}
		if (totalPerc.doubleValue() != 100.0D) {
			throw new Exception("Os percentuais configurados na tela 'ConfiguraRoyalties' estdiferente de 100%. Percentual total: " + BigDecimalUtil.toCurrency(totalPerc)); 
		}
		
		for (DynamicVO configVO : configsRoyaltiesVO) {
		      Collection<DynamicVO> tops = topDAO.find("CODTIPOPER = ?", configVO.asBigDecimal("CODTIPOPER"));
		      DynamicVO topVO = tops.iterator().next();
		      Element elemCabecalho = new Element("Cabecalho");
		      XMLUtils.addContentElement(elemCabecalho, "NUNOTA", "");
		      XMLUtils.addContentElement(elemCabecalho, "NUMNOTA", BigDecimal.ZERO);
		      XMLUtils.addContentElement(elemCabecalho, "CODEMP", configVO.asBigDecimal("CODEMPDEST"));
		      XMLUtils.addContentElement(elemCabecalho, "CODEMPNEGOC", configVO.asBigDecimal("CODEMPDEST"));
		      XMLUtils.addContentElement(elemCabecalho, "CODPARC", parVO.asBigDecimal("CODPARC"));
		      XMLUtils.addContentElement(elemCabecalho, "CODTIPOPER", topVO.asBigDecimal("CODTIPOPER"));
		      XMLUtils.addContentElement(elemCabecalho, "TIPMOV", topVO.asString("TIPMOV"));
		      XMLUtils.addContentElement(elemCabecalho, "CODTIPVENDA", cabVO.asBigDecimal("CODTIPVENDA"));
		      XMLUtils.addContentElement(elemCabecalho, "DTNEG", ddMMyyyySkw.format(cabVO.asTimestamp("DTNEG")));
		      XMLUtils.addContentElement(elemCabecalho, "CODCENCUS", configVO.asBigDecimal("CODCENCUS"));
		      XMLUtils.addContentElement(elemCabecalho, "CODNAT", configVO.asBigDecimal("CODNAT"));
		      XMLUtils.addContentElement(elemCabecalho, "CIF_FOB", "S");
		      XMLUtils.addContentElement(elemCabecalho, "AD_NUNOTA", cabVO.asBigDecimal("NUNOTA"));
		      XMLUtils.addContentElement(elemCabecalho, "AD_NUNOTAORIG", cabVO.asBigDecimal("NUNOTA"));
		      XMLUtils.addContentElement(elemCabecalho, "AD_STATUSPED", BigDecimal.valueOf(7L));
		      XMLUtils.addContentElement(elemCabecalho, "AD_CODUSUROY", usuarioLogado);
		      XMLUtils.addContentElement(elemCabecalho, "STATUSNOTA", String.valueOf("L"));
		      
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
		         if (nuNota == null) {
		        	 throw new Exception("Não foi possível gerar a nota"); 
		         }
		        
		         dwfEntityFacade.clearSessionCache("CabecalhoNota");
		         cacHelper.addIncluirAlterarListener((CACHelper.IncluirAlterarItemListener)new LoteAutomaticoHelper());
		         JapeSession.putProperty("ItemNota.incluindo.alterando.pela.central", Boolean.TRUE);
		         Element itensElem = new Element("itens");
		         itensElem.setAttribute("ATUALIZACAO_ONLINE", "false");
		         if (strNotas.length() > 0) {
		        	 strNotas.append(", "); 
		         }
		         strNotas.append(nuNota);
		         DynamicVO prodItemVO = proDAO.findByPK(configVO.asBigDecimal("CODPROD"));
		         
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
		         itensElem.addContent((Content)itemElem);
		     
		         BarramentoRegra.DadosBarramento dadosBarramento = cacHelper.incluirAlterarItem(nuNota, ServiceContext.getCurrent(), itensElem, false);
		         EntityPropertyDescriptor[] fdsItem = dwfEntityFacade.getDAOInstance("ItemNota").getSQLProvider().getPkObjectUID().getFieldDescriptors();
		         Collection<EntityPrimaryKey> pksEnvolvidasItem = dadosBarramento.getPksEnvolvidas();
		         BigDecimal sequencia = null;
		         
		         if (pksEnvolvidasItem != null && pksEnvolvidasItem.size() > 0) {
		        	 EntityPrimaryKey itemKey = pksEnvolvidasItem.iterator().next();
		        	 for (int j = 0; j < fdsItem.length; j++) {
		        		 EntityPropertyDescriptor itemEntity = fdsItem[j];
		        		 if ("SEQUENCIA".equals(itemEntity.getField().getName())) {
		        			 sequencia = new BigDecimal(itemKey.getValues()[j].toString());
		        		 }
		        	 } 
		         }
		         
		         
		         gravarDescontoCredito(parVO.asBigDecimal("CODPARC"), configVO.asBigDecimal("CODEMPDEST"), nuNota);
		      
		         Collection<Exception> erros = dadosBarramento.getErros();
		         if (erros.size() > 0) {
		        	 throw (Exception)erros.iterator().next(); 
		         }
		         
		         Collection<LiberacaoSolicitada> liberacoes = dadosBarramento.getLiberacoesSolicitadas();
		         if (liberacoes.size() > 0) {
		        	 throw new Exception("Nfoi possgerar a nota. Solicitade liberagerada: " + ((LiberacaoSolicitada)liberacoes.iterator().next()).getDescricao());
		         }
		         
		         Collection<ClientEvent> clientEvents = dadosBarramento.getClientEvents();
		         if (clientEvents.size() > 0) {
		        	 throw new Exception("Não foi possível gerar a nota. Evento solicitado: " + ((ClientEvent)clientEvents.iterator().next()).getEventID()); 
		         }
		         
		         if (sequencia == null) {
		        	 throw new Exception("Não foi possível gerar a nota. Nenhum item gerado."); 
		         }
		     }
		}
		
		cabVO.setProperty("AD_STATUSROYAL", "1");
		notaPersistent.setValueObject((EntityVO)cabVO);
			
		gravarMensagemSucesso(dwfEntityFacade, cabVO, strNotas);
		    
	}
	
	private void gravarDescontoCredito(BigDecimal codParc, BigDecimal codEmp, BigDecimal nuNota) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
		NativeSql nativeSql = null;
		try {
		  jdbc.openSession();
		  nativeSql = new NativeSql(jdbc);
		  BigDecimal percUsoPermitido = new BigDecimal(MGECoreParameter.getParameterAsInt("PERCPDUSACRED"));
					
		  if (BigDecimalUtil.isEmpty(percUsoPermitido)) {
			  throw new Exception("Parâmetro PERCPDUSACRED não está configurado."); 
		  }
		  
		  String strWhere = String.format("CODPARC = %s AND CODEMP = %s", new Object[] { codParc, codEmp });
		  BigDecimal vlrCredito = NativeSql.getBigDecimal("SUM( NVL(VLRCREDITO,0) - NVL(( SELECT SUM(VLRCONSUMO) FROM AD_CREDITOCONSUMO CO WHERE CO.NUCREDITO = CR.NUCREDITO ),0) )", "AD_CREDITOCLIENTE CR", strWhere);
		  if (BigDecimalUtil.isEmpty(vlrCredito)) {
			  return; 
		  }
		  
		  JapeWrapper pedidoDAO = JapeFactory.dao("CabecalhoNota");
		  DynamicVO pedidoVO = pedidoDAO.findByPK(new Object[] { nuNota });
		  if (pedidoVO == null) {
			  return; 
		  }
		  
		  BigDecimal limiteCreditoPD = pedidoVO.asBigDecimal("VLRNOTA").multiply(percUsoPermitido).divide(BigDecimalUtil.CEM_VALUE).setScale(2, RoundingMode.HALF_UP);
		  if (vlrCredito.compareTo(limiteCreditoPD) > 0) {
			  vlrCredito = limiteCreditoPD; 
		  }
		  
		  gravarDescontoItens(pedidoVO, vlrCredito);
		  
		  NumberFormat formatter = NumberFormat.getCurrencyInstance();
		  String strObservacao = String.format("Valor original do pedido %s\nValor desconto aplicado  %s\nValor liquido %s", 
				  								new Object[] { formatter.format(pedidoVO.asBigDecimal("VLRNOTA")), 
				  								formatter.format(vlrCredito), 
				  								formatter.format(pedidoVO.asBigDecimal("VLRNOTA").subtract(vlrCredito)) });
		  
		  DynamicVO NEWpedidoVO = pedidoDAO.findByPK(new Object[] { nuNota });
		  CentralFinanceiro centralFinanceiro = new CentralFinanceiro();
		  centralFinanceiro.excluiFinanceiro(nuNota);
		  ImpostosHelpper impostosHelper = new ImpostosHelpper();
		  impostosHelper.setForcarRecalculo(true);
		  impostosHelper.forcaRecalculoBaseISS(true);
		  impostosHelper.recalculoICMS(pedidoVO, pedidoVO);
		  ((FluidUpdateVO)((FluidUpdateVO)pedidoDAO.prepareToUpdate(pedidoVO).set("OBSERVACAO", strObservacao)).set("AD_VLRDESCPGTOAVISTA", vlrCredito)).update();
		  gravarConsumoCredito(pedidoVO, vlrCredito);
		    
		} finally {
		       JdbcWrapper.closeSession(jdbc);
		       NativeSql.releaseResources(nativeSql);
		} 
	}
	
	
	private void gravarDescontoItens(DynamicVO pedidoVO, BigDecimal vlrCredito) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
		jdbc.openSession();
		try {
			Collection<DynamicVO> itens = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper("ItemNota", "this.NUNOTA = ? ", new Object[] { pedidoVO.asBigDecimal("NUNOTA") }));
		    if (itens.isEmpty()) {
		    	return; 
		    }
		    if (pedidoVO.asBigDecimal("VLRNOTA").compareTo(BigDecimal.ZERO) == 0) {
		    	return; 
		    }
		    BigDecimal indiceDescCredito = vlrCredito.divide(pedidoVO.asBigDecimal("VLRNOTA"), MathContext.DECIMAL128).setScale(6, RoundingMode.HALF_UP);
		    JapeWrapper itemDAO = JapeFactory.dao("ItemNota");
		    BigDecimal descontoAplicado = BigDecimal.ZERO;
		    int itemNro = 0;
		    for (DynamicVO itemVO : itens) {
		        BigDecimal vlrDesconto = itemVO.asBigDecimal("VLRUNIT").multiply(indiceDescCredito).setScale(2, RoundingMode.HALF_UP);
		        BigDecimal vlrUnit = itemVO.asBigDecimal("VLRUNIT").subtract(vlrDesconto);
		        descontoAplicado = descontoAplicado.add(vlrDesconto);
		        itemNro++;
		        if (itemNro == itens.size()) {
		          BigDecimal resto = vlrCredito.subtract(descontoAplicado);
		          if (resto.compareTo(BigDecimal.ZERO) > 0) {
		            vlrUnit = vlrUnit.add(resto); 
		          }
		        }

		        BigDecimal vlrTot = vlrUnit.multiply(itemVO.asBigDecimal("QTDNEG")).setScale(2, RoundingMode.HALF_UP);
		        ((FluidUpdateVO)((FluidUpdateVO)itemDAO.prepareToUpdate(itemVO).set("VLRUNIT", vlrUnit)).set("VLRTOT", vlrTot)).update();
		     } 
		} catch (Exception e) {
		       throw new Exception("Erro gravando desconto itens.\n\n" + e.getMessage(), e);
		} finally {
		       JdbcWrapper.closeSession(jdbc);
		} 
	}
	
	
	private void gravarConsumoCredito(DynamicVO pedidoVO, BigDecimal totalConsumido) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
		jdbc.openSession();
		try {
			StringBuffer queryCredito = new StringBuffer(
		    NativeSql.loadSQLFromResource(getClass(), "creditoConsumir.sql"));
		    NativeSql nSql = new NativeSql(jdbc);
		    nSql.appendSql(queryCredito.toString());
		    nSql.setNamedParameter("CODPARC", pedidoVO.asBigDecimal("CODPARC"));
		    nSql.setNamedParameter("CODEMP", pedidoVO.asBigDecimal("CODEMP"));
		    ResultSet rs = nSql.executeQuery();
		    NativeSql insertSQL = new NativeSql(jdbc);
		    while (rs.next()) {
		    	BigDecimal vlrConsumo;
		         if (totalConsumido.compareTo(rs.getBigDecimal("VLRCREDITO")) < 0) {
		           vlrConsumo = totalConsumido;
		         } else {
		           vlrConsumo = rs.getBigDecimal("VLRCREDITO");
		         } 
		         insertSQL.executeUpdate(String.format(" INSERT INTO AD_CREDITOCONSUMO (NUCREDITO, NUNOTA, VLRCONSUMO) VALUES ( %s, %s, %s )", new Object[] { rs.getBigDecimal("NUCREDITO"), pedidoVO.asBigDecimal("NUNOTA"), vlrConsumo }));
		         totalConsumido = totalConsumido.subtract(vlrConsumo);
		         if (totalConsumido.compareTo(BigDecimal.ZERO) == 0) {
		           break; 
		         }
		    } 
		       
		    rs.close();
		 } catch (Exception e) {
			 DynamicVO mensagemVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("AD_MSGATUALROYALTIES");
		      mensagemVO.setProperty("NUNOTA", pedidoVO.asBigDecimal("NUNOTA"));
		      mensagemVO.setProperty("MENSAGEM", "Erro gravando consumo do cr\n\n" + e.getMessage());
		      mensagemVO.setProperty("DHMENSAGEM", TimeUtils.getNow());
		      dwfEntityFacade.createEntity("AD_MSGATUALROYALTIES", (EntityVO)mensagemVO);
		 } finally {
		       JdbcWrapper.closeSession(jdbc);
		 } 
	}
	
	private void gravarMensagemErro(EntityFacade dwfEntityFacade, DynamicVO notaOrigemVO, String mensagem) throws Exception {
		DynamicVO mensagemVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("AD_MSGATUALROYALTIES");
	    
		mensagemVO.setProperty("NUNOTA", notaOrigemVO.asBigDecimal("NUNOTA"));
	    mensagemVO.setProperty("MENSAGEM", (StringUtils.getNullAsEmpty(mensagem).length() > 3999) ? mensagem.substring(0, 3999) : mensagem);
		mensagemVO.setProperty("DHMENSAGEM", TimeUtils.getNow());
		mensagemVO.setProperty("TIPO", String.valueOf("E"));
		
		dwfEntityFacade.createEntity("AD_MSGATUALROYALTIES", (EntityVO)mensagemVO);
		PersistentLocalEntity notaPersistent = dwfEntityFacade.findEntityByPrimaryKey("CabecalhoNota", notaOrigemVO.asBigDecimal("NUNOTA"));
		notaOrigemVO.setProperty("AD_ERROROYALTIES", "S");
		notaPersistent.setValueObject((EntityVO)notaOrigemVO);
		erroAoCalcular = "S";
	}
	
	private void gravarMensagemSucesso(EntityFacade dwfEntityFacade, DynamicVO notaOrigemVO, StringBuffer strNotas) throws Exception {
		DynamicVO mensagemVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("AD_MSGATUALROYALTIES");
		mensagemVO.setProperty("NUNOTA", notaOrigemVO.asBigDecimal("NUNOTA"));
		mensagemVO.setProperty("MENSAGEM", String.format("Pedidos %s gerados com sucesso.", new Object[] { strNotas.toString() }));
		mensagemVO.setProperty("DHMENSAGEM", TimeUtils.getNow());
		mensagemVO.setProperty("TIPO", String.valueOf("I"));
		dwfEntityFacade.createEntity("AD_MSGATUALROYALTIES", (EntityVO)mensagemVO);
	}
	
}
