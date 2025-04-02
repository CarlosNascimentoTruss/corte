/*     */ package br.com.evolvesolucoes.truss;
/*     */ 
/*     */ import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
/*     */ import br.com.sankhya.extensions.actionbutton.ContextoAcao;
/*     */ import br.com.sankhya.extensions.actionbutton.Registro;
/*     */ import br.com.sankhya.jape.EntityFacade;
/*     */ import br.com.sankhya.jape.bmp.PersistentLocalEntity;
/*     */ import br.com.sankhya.jape.core.JapeSession;
/*     */ import br.com.sankhya.jape.dao.EntityPrimaryKey;
/*     */ import br.com.sankhya.jape.dao.EntityPropertyDescriptor;
/*     */ import br.com.sankhya.jape.dao.JdbcWrapper;
/*     */ import br.com.sankhya.jape.sql.NativeSql;
/*     */ import br.com.sankhya.jape.util.FinderWrapper;
/*     */ import br.com.sankhya.jape.util.JapeSessionContext;
/*     */ import br.com.sankhya.jape.vo.DynamicVO;
/*     */ import br.com.sankhya.jape.vo.EntityVO;
/*     */ import br.com.sankhya.jape.wrapper.JapeFactory;
/*     */ import br.com.sankhya.jape.wrapper.JapeWrapper;
/*     */ import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
/*     */ import br.com.sankhya.modelcore.auth.AuthenticationInfo;
/*     */ import br.com.sankhya.modelcore.comercial.BarramentoRegra;
/*     */ import br.com.sankhya.modelcore.comercial.CentralFinanceiro;
/*     */ import br.com.sankhya.modelcore.comercial.ClientEvent;
/*     */ import br.com.sankhya.modelcore.comercial.LiberacaoSolicitada;
/*     */ import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
/*     */ import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
/*     */ import br.com.sankhya.modelcore.comercial.util.LoteAutomaticoHelper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
/*     */ import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*     */ import br.com.sankhya.modelcore.util.MGECoreParameter;
/*     */ import br.com.sankhya.ws.ServiceContext;
/*     */ import com.sankhya.util.BigDecimalUtil;
/*     */ import com.sankhya.util.StringUtils;
/*     */ import com.sankhya.util.TimeUtils;
/*     */ import com.sankhya.util.XMLUtils;
/*     */ import java.math.BigDecimal;
/*     */ import java.math.MathContext;
/*     */ import java.math.RoundingMode;
/*     */ import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
/*     */ import java.text.NumberFormat;
/*     */ import java.text.SimpleDateFormat;
/*     */ import java.util.Collection;
/*     */ import org.cuckoo.core.ScheduledAction;
/*     */ import org.cuckoo.core.ScheduledActionContext;
/*     */ import org.jdom.Content;
/*     */ import org.jdom.Element;
/*     */ 
/*     */ 
/*     */ public class BotaoGerarRoyalties
/*     */   implements AcaoRotinaJava, ScheduledAction
/*     */ {
/*  51 */   private static final SimpleDateFormat ddMMyyyySkw = new SimpleDateFormat("dd/MM/yyyy");
/*     */   String erroAoCalcular = "N";
			DynamicVO cabVO = null;
/*     */   public void doAction(ContextoAcao contexto) throws Exception {
			  
			  validaGeracaoRoyalties validaRoyalties = new validaGeracaoRoyalties();
			  
			  
			  
/*  54 */     EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/*  55 */     if ((contexto.getLinhas()).length == 0) {
/*  56 */       contexto.mostraErro("Selecione uma nota antes.");
/*  57 */     } else if ((contexto.getLinhas()).length > 1) {
/*  58 */       contexto.mostraErro("Selecione apenas uma nota de cada vez.");
/*     */     } 
/*  60 */     contexto.confirmar("GeraRoyalties", 
/*  61 */         "Esta opção vai gerar uma nota Royalty/Taxa de publicidade.<br>Deseja continuar?", 1);
/*  62 */     Registro linha = contexto.getLinhas()[0];
				
				BigDecimal nunota = (BigDecimal) linha.getCampo("NUNOTA");
				String tipmov = (String) linha.getCampo("TIPMOV");
				String statusNfe = (String) linha.getCampo("STATUSNFE");
				
				if(!tipmov.equals("V") || !statusNfe.equals("A")) {
					contexto.mostraErro("Apenas notas do tipo Venda e com Status NFe Aprovada podem gerar royalties.");
				}
				
				if(validaRoyalties.qtdItensGeraRoyalties(nunota).equals(BigDecimal.ZERO)) {
					contexto.mostraErro("Não há itens disponíveis para o cálculo de Royalties.");
				}
				
/*  63 */     PersistentLocalEntity notaPersistent = dwfEntityFacade.findEntityByPrimaryKey("CabecalhoNota", 
/*  64 */         linha.getCampo("NUNOTA"));
/*  65 */     DynamicVO notaOrigemVO = (DynamicVO)notaPersistent.getValueObject();
/*  66 */     DynamicVO parceiroVO = notaOrigemVO.asDymamicVO("Parceiro");
/*  67 */     //if ("1".equals(notaOrigemVO.asString("AD_STATUSROYAL")))
/*  68 */       //contexto.mostraErro("Royaltes jgerados, nserposscontinuar."); 
/*  69 */     if (!"L".equals(notaOrigemVO.asString("STATUSNOTA")))
/*  70 */       contexto.mostraErro("Para gerar os royaltes, a nota deve estar confirmada."); 
/*  71 */     BigDecimal codEmpRoyalties = parceiroVO.asBigDecimal("AD_EMPROYALTIES");
/*  72 */     if (codEmpRoyalties == null)
/*  73 */       contexto.mostraErro(String.format("Nconfigurado a Empresa de Royalties para o parceiro ", 
/*  74 */             new Object[] { parceiroVO.asBigDecimal("CODPARC"), parceiroVO.asString("NOMEPARC") })); 
/*  75 */     BigDecimal codEmpTaxa = parceiroVO.asBigDecimal("AD_EMPTAXA");
/*  76 */     if (codEmpTaxa == null)
/*  77 */       contexto.mostraErro(String.format("Nconfigurado a Empresa de Taxa Publicidade para o parceiro ", 
/*  78 */             new Object[] { parceiroVO.asBigDecimal("CODPARC"), parceiroVO.asString("NOMEPARC") })); 
/*  79 */     Collection<DynamicVO> configuracoes = dwfEntityFacade.findByDynamicFinderAsVO(
/*  80 */         new FinderWrapper("AD_CONFIGROYALTIESSERV", "this.CODEMP = ? AND this.CODEMPDEST IN ( ?, ? )", 
/*  81 */           new Object[] { notaOrigemVO.asBigDecimal("CODEMP"), codEmpRoyalties, codEmpTaxa }));
/*  82 */     if (configuracoes.size() == 0)
/*  83 */       contexto.mostraErro(String.format("Nfoi localizada na tela 'ConfiguraRoyalties' a configurapara a empresa ", 
/*  84 */             new Object[] { notaOrigemVO.asBigDecimal("CODEMP"), 
/*  85 */               notaOrigemVO.asString("Empresa.RAZAOSOCIAL") })); 
/*  86 */     if (parceiroVO.asBigDecimal("AD_CODTABSERV") == null)
/*  87 */       contexto.mostraErro(
/*  88 */           String.format("Nfoi localizada 'Tabela de prepara servina tela 'Parceiros' para o parceiro ", 
/*  89 */             new Object[] { parceiroVO.asBigDecimal("CODPARC"), parceiroVO.asString("NOMEPARC") })); 
/*  90 */     boolean booCalcularRoyaltiesPorParceiro = "S".equals(MGECoreParameter.getParameterAsString("CALCROYPARC"));
/*  91 */     BigDecimal vlrTotal = null;



/*  92 */     if (booCalcularRoyaltiesPorParceiro) {
/*  93 */       vlrTotal = NativeSql.getBigDecimal("SUM(VLRTOT - VLRDESC) * ? ", "TGFITE", "USOPROD != 'D' AND NUNOTA = ? AND NVL(AD_CALCSERVICO,'N') = 'N'", 
/*  94 */           new Object[] { parceiroVO.asBigDecimal("AD_PERCSERVICOS"), notaOrigemVO.asBigDecimal("NUNOTA") });
/*     */     } else {
/*  96 */       vlrTotal = NativeSql.getBigDecimal("SUM(QTDNEG * SNK_PRECO(?, CODPROD))", "TGFITE", 
/*  97 */           "USOPROD != 'D' AND NUNOTA = ? AND NVL(AD_DUZIA, 'N') = 'N' AND NVL(AD_CALCSERVICO,'N') = 'N'", 
/*  98 */           new Object[] { parceiroVO.asBigDecimal("AD_CODTABSERV"), notaOrigemVO.asBigDecimal("NUNOTA") });
/*     */     } 




/* 100 */     if (vlrTotal == null)
/* 101 */       contexto.mostraErro("Nota ntem itens que atendam a regra de gerade royalties."); 
/* 102 */     vlrTotal = BigDecimalUtil.getRounded(vlrTotal, 2);
/* 103 */     CACHelper cacHelper = new CACHelper();
/* 104 */     JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
/* 105 */     StringBuffer strNotas = new StringBuffer();
/* 106 */     BigDecimal totalPerc = BigDecimal.ZERO;
/* 107 */     for (DynamicVO configVO : configuracoes)
/* 108 */       totalPerc = totalPerc.add(configVO.asBigDecimalOrZero("PERCTAXA")); 
/* 109 */     if (totalPerc.doubleValue() != 100.0D)
/* 110 */       contexto.mostraErro(
/* 111 */           "Os percentuais configurados na tela 'ConfiguraRoyalties' estdiferente de 100%. Percentual total: " + 
/* 112 */           BigDecimalUtil.toCurrency(totalPerc)); 
/* 113 */     for (DynamicVO configVO : configuracoes) {
/* 114 */       Collection<DynamicVO> tops = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper("TipoOperacao", 
/* 115 */             "this.CODTIPOPER = ?", new Object[] { configVO.asBigDecimal("CODTIPOPER") }));
/* 116 */       DynamicVO topVO = tops.iterator().next();
/* 117 */       Element elemCabecalho = new Element("Cabecalho");
/* 118 */       XMLUtils.addContentElement(elemCabecalho, "NUNOTA", "");
/* 119 */       XMLUtils.addContentElement(elemCabecalho, "NUMNOTA", BigDecimal.ZERO);
/* 120 */       XMLUtils.addContentElement(elemCabecalho, "CODEMP", configVO.asBigDecimal("CODEMPDEST"));
/* 121 */       XMLUtils.addContentElement(elemCabecalho, "CODEMPNEGOC", configVO.asBigDecimal("CODEMPDEST"));
/* 122 */       XMLUtils.addContentElement(elemCabecalho, "CODPARC", parceiroVO.asBigDecimal("CODPARC"));
/* 123 */       XMLUtils.addContentElement(elemCabecalho, "CODTIPOPER", topVO.asBigDecimal("CODTIPOPER"));
/* 124 */       XMLUtils.addContentElement(elemCabecalho, "TIPMOV", topVO.asString("TIPMOV"));
/* 125 */       XMLUtils.addContentElement(elemCabecalho, "CODTIPVENDA", notaOrigemVO.asBigDecimal("CODTIPVENDA"));
/* 126 */       XMLUtils.addContentElement(elemCabecalho, "DTNEG", ddMMyyyySkw.format(notaOrigemVO.asTimestamp("DTNEG")));
/* 127 */       XMLUtils.addContentElement(elemCabecalho, "CODCENCUS", configVO.asBigDecimal("CODCENCUS"));
/* 128 */       XMLUtils.addContentElement(elemCabecalho, "CODNAT", configVO.asBigDecimal("CODNAT"));
/* 129 */       XMLUtils.addContentElement(elemCabecalho, "CIF_FOB", "S");
/* 130 */       XMLUtils.addContentElement(elemCabecalho, "AD_NUNOTA", notaOrigemVO.asBigDecimal("NUNOTA"));
                XMLUtils.addContentElement(elemCabecalho, "AD_NUNOTAORIG", notaOrigemVO.asBigDecimal("NUNOTA"));
/* 131 */       XMLUtils.addContentElement(elemCabecalho, "AD_STATUSPED", BigDecimal.valueOf(7L));
/* 132 */       XMLUtils.addContentElement(elemCabecalho, "AD_CODUSUROY", contexto.getUsuarioLogado());
/* 133 */       XMLUtils.addContentElement(elemCabecalho, "STATUSNOTA", String.valueOf("L"));
/* 134 */       BarramentoRegra barra = cacHelper.incluirAlterarCabecalho(ServiceContext.getCurrent(), elemCabecalho);
/* 135 */       EntityPropertyDescriptor[] fds = barra.getState().getDao().getSQLProvider().getPkObjectUID()
/* 136 */         .getFieldDescriptors();
/* 137 */       Collection<EntityPrimaryKey> pksEnvolvidas = barra.getDadosBarramento().getPksEnvolvidas();
/* 138 */       EntityPrimaryKey cabKey = pksEnvolvidas.iterator().next();
/* 139 */       BigDecimal nuNota = null;
/* 140 */       for (int i = 0; i < fds.length; i++) {
/* 141 */         EntityPropertyDescriptor cabEntity = fds[i];
/* 142 */         if ("NUNOTA".equals(cabEntity.getField().getName()))
/* 143 */           nuNota = new BigDecimal(cabKey.getValues()[i].toString()); 
/*     */       } 
/* 145 */       if (nuNota == null)
/* 146 */         throw new Exception("Nfoi possgerar a nota"); 
/* 147 */       dwfEntityFacade.clearSessionCache("CabecalhoNota");
/* 148 */       cacHelper.addIncluirAlterarListener((CACHelper.IncluirAlterarItemListener)new LoteAutomaticoHelper());
/* 149 */       JapeSession.putProperty("ItemNota.incluindo.alterando.pela.central", Boolean.TRUE);
/* 150 */       Element itensElem = new Element("itens");
/* 151 */       itensElem.setAttribute("ATUALIZACAO_ONLINE", "false");
/* 152 */       if (strNotas.length() > 0)
/* 153 */         strNotas.append(", "); 
/* 154 */       strNotas.append(nuNota);
/* 155 */       DynamicVO prodItemVO = (DynamicVO)dwfEntityFacade.findEntityByPrimaryKeyAsVO("Servico", 
/* 156 */           new Object[] { configVO.asBigDecimal("CODPROD") });
/* 157 */       BigDecimal valorUnitario = 
/* 158 */         BigDecimalUtil.getRounded(vlrTotal.multiply(configVO.asBigDecimalOrZero("PERCTAXA"))
/* 159 */           .divide(BigDecimalUtil.CEM_VALUE, BigDecimalUtil.MATH_CTX), prodItemVO.asInt("DECVLR"));
/* 160 */       Element itemElem = new Element("item");
/* 161 */       XMLUtils.addContentElement(itemElem, "NUNOTA", nuNota);
/* 162 */       XMLUtils.addContentElement(itemElem, "SEQUENCIA", "");
/* 163 */       XMLUtils.addContentElement(itemElem, "CODPROD", prodItemVO.asBigDecimal("CODPROD"));
/* 164 */       XMLUtils.addContentElement(itemElem, "CODVOL", prodItemVO.asString("CODVOL"));
/* 165 */       XMLUtils.addContentElement(itemElem, "QTDNEG", BigDecimal.ONE);
/* 166 */       XMLUtils.addContentElement(itemElem, "PERCDESC", BigDecimal.ZERO);
/* 167 */       XMLUtils.addContentElement(itemElem, "VLRDESC", BigDecimal.ZERO);
/* 168 */       XMLUtils.addContentElement(itemElem, "VLRUNIT", valorUnitario);
/* 169 */       XMLUtils.addContentElement(itemElem, "VLRTOT", valorUnitario);
/* 170 */       itensElem.addContent((Content)itemElem);
/* 171 */       BarramentoRegra.DadosBarramento dadosBarramento = cacHelper.incluirAlterarItem(nuNota, 
/* 172 */           ServiceContext.getCurrent(), itensElem, false);
/* 173 */       EntityPropertyDescriptor[] fdsItem = dwfEntityFacade.getDAOInstance("ItemNota").getSQLProvider()
/* 174 */         .getPkObjectUID().getFieldDescriptors();
/* 175 */       Collection<EntityPrimaryKey> pksEnvolvidasItem = dadosBarramento.getPksEnvolvidas();
/* 176 */       BigDecimal sequencia = null;
/* 177 */       if (pksEnvolvidasItem != null && pksEnvolvidasItem.size() > 0) {
/* 178 */         EntityPrimaryKey itemKey = pksEnvolvidasItem.iterator().next();
/* 179 */         for (int j = 0; j < fdsItem.length; j++) {
/* 180 */           EntityPropertyDescriptor itemEntity = fdsItem[j];
/* 181 */           if ("SEQUENCIA".equals(itemEntity.getField().getName()))
/* 182 */             sequencia = new BigDecimal(itemKey.getValues()[j].toString()); 
/*     */         } 
/*     */       } 
/* 185 */       gravarDescontoCredito(parceiroVO.asBigDecimal("CODPARC"), configVO.asBigDecimal("CODEMPDEST"), nuNota);
/* 186 */       Collection<Exception> erros = dadosBarramento.getErros();
/* 187 */       if (erros.size() > 0)
/* 188 */         throw (Exception)erros.iterator().next(); 
/* 189 */       Collection<LiberacaoSolicitada> liberacoes = dadosBarramento.getLiberacoesSolicitadas();
/* 190 */       if (liberacoes.size() > 0)
/* 191 */         throw new Exception("Nfoi possgerar a nota. Solicitade liberagerada: " + (
/* 192 */             (LiberacaoSolicitada)liberacoes.iterator().next()).getDescricao()); 
/* 193 */       Collection<ClientEvent> clientEvents = dadosBarramento.getClientEvents();
/* 194 */       if (clientEvents.size() > 0)
/* 195 */         throw new Exception("Nfoi possgerar a nota. Evento solicitado: " + (
/* 196 */             (ClientEvent)clientEvents.iterator().next()).getEventID()); 
/* 197 */       if (sequencia == null)
/* 198 */         throw new Exception("Nfoi possgerar a nota. Nenhum item gerado."); 
/*     */     } 
/* 200 */     notaOrigemVO.setProperty("AD_STATUSROYAL", "1");
/* 201 */     notaPersistent.setValueObject((EntityVO)notaOrigemVO);
			  try {
				validaRoyalties.atualizaItens(nunota);
			  } catch (Exception e) {
				  contexto.mostraErro("Falha ao atualizar itens: " + e.getMessage());
			  }
			  gravarMensagemSucesso(dwfEntityFacade, notaOrigemVO, strNotas);
/* 202 */     contexto.setMensagemRetorno(
/* 203 */         String.format("Pedidos %s gerados com sucesso.", new Object[] { strNotas.toString() }));
/*     */   }
/*     */   
/*     */   private void gravarDescontoCredito(BigDecimal codParc, BigDecimal codEmp, BigDecimal nuNota) throws Exception {
/* 207 */     EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/* 208 */     JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
/* 209 */     NativeSql nativeSql = null;
/*     */     try {
/* 211 */       jdbc.openSession();
/* 212 */       nativeSql = new NativeSql(jdbc);
/* 213 */       BigDecimal percUsoPermitido = new BigDecimal(MGECoreParameter.getParameterAsInt("PERCPDUSACRED"));
			
/* 214 */       if (BigDecimalUtil.isEmpty(percUsoPermitido))
/* 215 */         throw new Exception("ParPERCPDUSACRED nestconfigurado."); 
/* 216 */       String strWhere = String.format("CODPARC = %s AND CODEMP = %s", new Object[] { codParc, codEmp });
/* 217 */       BigDecimal vlrCredito = NativeSql.getBigDecimal(
/* 218 */           "SUM( NVL(VLRCREDITO,0) - NVL(( SELECT SUM(VLRCONSUMO) FROM AD_CREDITOCONSUMO CO WHERE CO.NUCREDITO = CR.NUCREDITO ),0) )", 
/* 219 */           "AD_CREDITOCLIENTE CR", strWhere);
/* 220 */       if (BigDecimalUtil.isEmpty(vlrCredito))
/*     */         return; 
/* 222 */       JapeWrapper pedidoDAO = JapeFactory.dao("CabecalhoNota");
/* 223 */       DynamicVO pedidoVO = pedidoDAO.findByPK(new Object[] { nuNota });
/* 224 */       if (pedidoVO == null)
/*     */         return; 
/* 226 */       BigDecimal limiteCreditoPD = pedidoVO.asBigDecimal("VLRNOTA").multiply(percUsoPermitido)
/* 227 */         .divide(BigDecimalUtil.CEM_VALUE).setScale(2, RoundingMode.HALF_UP);
/* 228 */       if (vlrCredito.compareTo(limiteCreditoPD) > 0)
/* 229 */         vlrCredito = limiteCreditoPD; 
/* 230 */       gravarDescontoItens(pedidoVO, vlrCredito);
/* 231 */       NumberFormat formatter = NumberFormat.getCurrencyInstance();
/* 232 */       String strObservacao = 
/* 233 */         String.format("Valor original do pedido %s\nValor desconto aplicado  %s\nValor liquido            %s", 
/* 234 */           new Object[] { formatter.format(pedidoVO.asBigDecimal("VLRNOTA")), 
/* 235 */             formatter.format(vlrCredito), 
/* 236 */             formatter.format(pedidoVO.asBigDecimal("VLRNOTA").subtract(vlrCredito)) });
/* 237 */       DynamicVO NEWpedidoVO = pedidoDAO.findByPK(new Object[] { nuNota });
/* 238 */       CentralFinanceiro centralFinanceiro = new CentralFinanceiro();
/* 239 */       centralFinanceiro.excluiFinanceiro(nuNota);
/* 240 */       ImpostosHelpper impostosHelper = new ImpostosHelpper();
/* 241 */       impostosHelper.setForcarRecalculo(true);
/* 242 */       impostosHelper.forcaRecalculoBaseISS(true);
/* 243 */       impostosHelper.recalculoICMS(pedidoVO, pedidoVO);
/* 244 */       ((FluidUpdateVO)((FluidUpdateVO)pedidoDAO.prepareToUpdate(pedidoVO).set("OBSERVACAO", strObservacao))
/* 245 */         .set("AD_VLRDESCPGTOAVISTA", vlrCredito)).update();
/* 246 */       gravarConsumoCredito(pedidoVO, vlrCredito);
/*     */     } finally {
/* 248 */       JdbcWrapper.closeSession(jdbc);
/* 249 */       NativeSql.releaseResources(nativeSql);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void gravarDescontoItens(DynamicVO pedidoVO, BigDecimal vlrCredito) throws Exception {
/* 254 */     EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/* 255 */     JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
/* 256 */     jdbc.openSession();
/*     */     try {
/* 258 */       Collection<DynamicVO> itens = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper("ItemNota", 
/* 259 */             "this.NUNOTA = ? ", new Object[] { pedidoVO.asBigDecimal("NUNOTA") }));
/* 260 */       if (itens.isEmpty())
/*     */         return; 
/* 262 */       if (pedidoVO.asBigDecimal("VLRNOTA").compareTo(BigDecimal.ZERO) == 0)
/*     */         return; 
/* 264 */       BigDecimal indiceDescCredito = vlrCredito.divide(pedidoVO.asBigDecimal("VLRNOTA"), MathContext.DECIMAL128)
/* 265 */         .setScale(6, RoundingMode.HALF_UP);
/* 266 */       JapeWrapper itemDAO = JapeFactory.dao("ItemNota");
/* 267 */       BigDecimal descontoAplicado = BigDecimal.ZERO;
/* 268 */       int itemNro = 0;
/* 269 */       for (DynamicVO itemVO : itens) {
/* 270 */         BigDecimal vlrDesconto = itemVO.asBigDecimal("VLRUNIT").multiply(indiceDescCredito).setScale(2, 
/* 271 */             RoundingMode.HALF_UP);
/* 272 */         BigDecimal vlrUnit = itemVO.asBigDecimal("VLRUNIT").subtract(vlrDesconto);
/* 273 */         descontoAplicado = descontoAplicado.add(vlrDesconto);
/* 274 */         itemNro++;
/* 275 */         if (itemNro == itens.size()) {
/* 276 */           BigDecimal resto = vlrCredito.subtract(descontoAplicado);
/* 277 */           if (resto.compareTo(BigDecimal.ZERO) > 0)
/* 278 */             vlrUnit = vlrUnit.add(resto); 
/*     */         } 
/* 280 */         BigDecimal vlrTot = vlrUnit.multiply(itemVO.asBigDecimal("QTDNEG")).setScale(2, RoundingMode.HALF_UP);
/* 281 */         ((FluidUpdateVO)((FluidUpdateVO)itemDAO.prepareToUpdate(itemVO).set("VLRUNIT", vlrUnit)).set("VLRTOT", 
/* 282 */             vlrTot)).update();
/*     */       } 
/* 284 */     } catch (Exception e) {
/* 285 */       throw new Exception("Erro gravando desconto itens.\n\n" + e.getMessage(), e);
/*     */     } finally {
/* 287 */       JdbcWrapper.closeSession(jdbc);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void gravarConsumoCredito(DynamicVO pedidoVO, BigDecimal totalConsumido) throws Exception {
/* 292 */     EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/* 293 */     JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
/* 294 */     jdbc.openSession();
/*     */     try {
/* 296 */       StringBuffer queryCredito = new StringBuffer(
/* 297 */           NativeSql.loadSQLFromResource(getClass(), "creditoConsumir.sql"));
/* 298 */       NativeSql nSql = new NativeSql(jdbc);
/* 299 */       nSql.appendSql(queryCredito.toString());
/* 300 */       nSql.setNamedParameter("CODPARC", pedidoVO.asBigDecimal("CODPARC"));
/* 301 */       nSql.setNamedParameter("CODEMP", pedidoVO.asBigDecimal("CODEMP"));
/* 302 */       ResultSet rs = nSql.executeQuery();
/* 303 */       NativeSql insertSQL = new NativeSql(jdbc);
/* 304 */       while (rs.next()) {
/*     */         BigDecimal vlrConsumo;
/* 306 */         if (totalConsumido.compareTo(rs.getBigDecimal("VLRCREDITO")) < 0) {
/* 307 */           vlrConsumo = totalConsumido;
/*     */         } else {
/* 309 */           vlrConsumo = rs.getBigDecimal("VLRCREDITO");
/*     */         } 
/* 311 */         insertSQL.executeUpdate(String.format(
/* 312 */               " INSERT INTO AD_CREDITOCONSUMO (NUCREDITO, NUNOTA, VLRCONSUMO) VALUES ( %s, %s, %s )", 
/* 313 */               new Object[] { rs.getBigDecimal("NUCREDITO"), pedidoVO.asBigDecimal("NUNOTA"), vlrConsumo }));
/* 314 */         totalConsumido = totalConsumido.subtract(vlrConsumo);
/* 315 */         if (totalConsumido.compareTo(BigDecimal.ZERO) == 0)
/*     */           break; 
/*     */       } 
/* 318 */       rs.close();
/* 319 */     } catch (Exception e) {
/* 320 */       DynamicVO mensagemVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("AD_MSGATUALROYALTIES");
/* 321 */       mensagemVO.setProperty("NUNOTA", pedidoVO.asBigDecimal("NUNOTA"));
/* 322 */       mensagemVO.setProperty("MENSAGEM", "Erro gravando consumo do cr\n\n" + e.getMessage());
/* 323 */       mensagemVO.setProperty("DHMENSAGEM", TimeUtils.getNow());
/* 324 */       dwfEntityFacade.createEntity("AD_MSGATUALROYALTIES", (EntityVO)mensagemVO);
/*     */     } finally {
/* 326 */       JdbcWrapper.closeSession(jdbc);
/*     */     } 
/*     */   }
/*     */   
/*     */   public void onTime(ScheduledActionContext arg0)  {
			try {
/* 331 */     JdbcWrapper jdbc = null;
/* 332 */     JapeSession.SessionHandle hdn = null;
/* 333 */     EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/* 334 */     JapeSession.SessionHandle hnd = null;
/* 335 */     jdbc = dwfEntityFacade.getJdbcWrapper();
/* 336 */     setupContext();
				validaGeracaoRoyalties validaRoyalties = new validaGeracaoRoyalties();
/* 337 */     NativeSql queNotas = new NativeSql(jdbc);
				Timestamp dtInicCalcRoyal = null;
			  try {
				dtInicCalcRoyal =  (Timestamp) MGECoreParameter.getParameter("DTINICCALCROYAL");
			  } catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			  }
/* 338 */     /*queNotas.appendSql("select * from AD_VWROYALTIES a where not exists (select 1 from AD_MSGATUALROYALTIES m where m.nunota = a.nunota)");*/
			   queNotas.setNamedParameter("P_DTNEG", dtInicCalcRoyal);
			   queNotas.appendSql("select * from ad_vw_calcularroyalties WHERE DTNEG >= :P_DTNEG");

				
/*     */     //try {
/* 341 */       ResultSet rsGerarRoyalties = queNotas.executeQuery();
/* 342 */       while (rsGerarRoyalties.next()) {
				try {
				  erroAoCalcular = "N";
/* 343 */         BigDecimal nuNota = rsGerarRoyalties.getBigDecimal("NUNOTA");
				  System.out.println("Calculando Royalties do nro unico: " + nuNota);
/* 344 */         BigDecimal nuNotaPedidoProdutos = rsGerarRoyalties.getBigDecimal("NUNOTA");
/* 345 */         PersistentLocalEntity notaPersistent = dwfEntityFacade.findEntityByPrimaryKey("CabecalhoNota", nuNota);
/* 346 */         DynamicVO notaOrigemVO = (DynamicVO)notaPersistent.getValueObject();
/* 347 */         DynamicVO parceiroVO = notaOrigemVO.asDymamicVO("Parceiro");
/*     */         cabVO = notaOrigemVO;
/* 349 */         //if ("1".equals(notaOrigemVO.asString("AD_STATUSROYAL"))) {
/* 350 */          // gravarMensagemErro(dwfEntityFacade, notaOrigemVO, "Royaltes jgerados, nserposscontinuar");
/*     */         //}
/*     */         
/* 353 */         if (!"L".equals(notaOrigemVO.asString("STATUSNOTA"))) {
/* 354 */           gravarMensagemErro(dwfEntityFacade, notaOrigemVO, 
/* 355 */               "Para gerar os royaltes, a nota deve estar confirmada.");
/*     */         }
/*     */         
/* 358 */         BigDecimal codEmpRoyalties = parceiroVO.asBigDecimal("AD_EMPROYALTIES");
/*     */         
/* 360 */         if (codEmpRoyalties == null) {
/* 361 */           gravarMensagemErro(dwfEntityFacade, notaOrigemVO, 
/* 362 */               "Não configurada a Empresa de Royalties para o parceiro");
/*     */         }
/*     */ 
/*     */         
/* 366 */         BigDecimal codEmpTaxa = parceiroVO.asBigDecimal("AD_EMPTAXA");
/*     */         
/* 368 */         if (codEmpTaxa == null) {
/* 369 */           gravarMensagemErro(dwfEntityFacade, notaOrigemVO, 
/* 370 */               "Não configurada a Empresa de Taxa Publicidade para o parceiro");
/*     */         }
/*     */         
/* 373 */         Collection<DynamicVO> configuracoes = dwfEntityFacade.findByDynamicFinderAsVO(
/* 374 */             new FinderWrapper("AD_CONFIGROYALTIESSERV", "this.CODEMP = ? AND this.CODEMPDEST IN ( ?, ? )", 
/* 375 */               new Object[] { notaOrigemVO.asBigDecimal("CODEMP"), codEmpRoyalties, codEmpTaxa }));
/*     */         
/* 377 */         if (configuracoes.size() == 0) {
/* 378 */           gravarMensagemErro(dwfEntityFacade, notaOrigemVO, 
/* 379 */               "Não foi localizada na tela 'ConfiguraRoyalties' a configurapara a empresa ");
/*     */         }
/*     */         
/* 382 */         if (parceiroVO.asBigDecimal("AD_CODTABSERV") == null) {
/* 383 */           gravarMensagemErro(dwfEntityFacade, notaOrigemVO, 
/* 384 */               "Não foi localizada 'Tabela de prepara servina tela 'Parceiros' para o parceiro ");
/*     */         }
/*     */         
/* 387 */         boolean booCalcularRoyaltiesPorParceiro = "S"
/* 388 */           .equals(MGECoreParameter.getParameterAsString("CALCROYPARC"));
/*     */         
/* 390 */         BigDecimal vlrTotal = null;
/*     */         
/* 392 */         if (booCalcularRoyaltiesPorParceiro) {
/* 393 */           vlrTotal = NativeSql.getBigDecimal("SUM(VLRTOT - VLRDESC) * ? ", "TGFITE", 
/* 394 */               "USOPROD != 'D' AND NUNOTA = ? AND NVL(AD_CALCSERVICO,'N') = 'N'", new Object[] { parceiroVO.asBigDecimal("AD_PERCSERVICOS"), 
/* 395 */                 notaOrigemVO.asBigDecimal("NUNOTA") });
/*     */         } else {
/* 397 */           vlrTotal = NativeSql.getBigDecimal("SUM(QTDNEG * SNK_PRECO(?, CODPROD))", "TGFITE", 
/* 398 */               "USOPROD != 'D' AND NUNOTA = ? AND NVL(AD_DUZIA, 'N') = 'N' AND NVL(AD_CALCSERVICO,'N') = 'N'", new Object[] {
/* 399 */                 parceiroVO.asBigDecimal("AD_CODTABSERV"), notaOrigemVO.asBigDecimal("NUNOTA")
/*     */               });
/*     */         } 
					
				System.out.println("Valor total do nro unico " + nuNota+ ": " + vlrTotal);
			
/* 402 */         if (vlrTotal == null) {
/* 403 */           gravarMensagemErro(dwfEntityFacade, notaOrigemVO, 
/* 404 */               "Nota nãpossui itens que atendam a regra de geração de royalties.");
/*     */         }
/*     */         
/* 407 */         vlrTotal = BigDecimalUtil.getRounded(vlrTotal, 2);
/*     */         
/* 409 */         CACHelper cacHelper = new CACHelper();
/* 410 */         JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
/* 411 */         StringBuffer strNotas = new StringBuffer();
/* 412 */         BigDecimal totalPerc = BigDecimal.ZERO;
/*     */         
/* 414 */         for (DynamicVO configVO : configuracoes) {
/* 415 */           totalPerc = totalPerc.add(configVO.asBigDecimalOrZero("PERCTAXA"));
/*     */         }
/*     */         
/* 418 */         if (totalPerc.doubleValue() != 100.0D) {
/* 419 */           gravarMensagemErro(dwfEntityFacade, notaOrigemVO, 
/* 420 */               "Os percentuais configurados na tela 'ConfiguraRoyalties' está diferente de 100%. Percentual total: " + 
/* 421 */               BigDecimalUtil.toCurrency(totalPerc));
/*     */         }
/*     */         
/* 424 */         for (DynamicVO configVO : configuracoes) {
/* 425 */           Collection<DynamicVO> tops = dwfEntityFacade
/* 426 */             .findByDynamicFinderAsVO(new FinderWrapper("TipoOperacao", "this.CODTIPOPER = ?", 
/* 427 */                 new Object[] { configVO.asBigDecimal("CODTIPOPER") }));
/* 428 */           DynamicVO topVO = tops.iterator().next();
/* 429 */           Element elemCabecalho = new Element("Cabecalho");
/* 430 */           XMLUtils.addContentElement(elemCabecalho, "NUNOTA", "");
/* 431 */           XMLUtils.addContentElement(elemCabecalho, "NUMNOTA", BigDecimal.ZERO);
/* 432 */           XMLUtils.addContentElement(elemCabecalho, "CODEMP", configVO.asBigDecimal("CODEMPDEST"));
/* 433 */           XMLUtils.addContentElement(elemCabecalho, "CODEMPNEGOC", configVO.asBigDecimal("CODEMPDEST"));
/* 434 */           XMLUtils.addContentElement(elemCabecalho, "CODPARC", parceiroVO.asBigDecimal("CODPARC"));
/* 435 */           XMLUtils.addContentElement(elemCabecalho, "CODTIPOPER", topVO.asBigDecimal("CODTIPOPER"));
/* 436 */           XMLUtils.addContentElement(elemCabecalho, "TIPMOV", topVO.asString("TIPMOV"));
/* 437 */           XMLUtils.addContentElement(elemCabecalho, "CODTIPVENDA", notaOrigemVO.asBigDecimal("CODTIPVENDA"));
/* 438 */           XMLUtils.addContentElement(elemCabecalho, "DTNEG", 
/* 439 */               ddMMyyyySkw.format(notaOrigemVO.asTimestamp("DTNEG")));
/* 440 */           XMLUtils.addContentElement(elemCabecalho, "CODCENCUS", configVO.asBigDecimal("CODCENCUS"));
/* 441 */           XMLUtils.addContentElement(elemCabecalho, "CODNAT", configVO.asBigDecimal("CODNAT"));
/* 442 */           XMLUtils.addContentElement(elemCabecalho, "CIF_FOB", "S");
/* 443 */           XMLUtils.addContentElement(elemCabecalho, "AD_NUNOTA", notaOrigemVO.asBigDecimal("NUNOTA"));
/* 444 */           XMLUtils.addContentElement(elemCabecalho, "AD_STATUSPED", BigDecimal.valueOf(7L));
/* 445 */           XMLUtils.addContentElement(elemCabecalho, "AD_NUNOTAORIG", nuNotaPedidoProdutos);
/*     */           
/* 447 */           BarramentoRegra barra = cacHelper.incluirAlterarCabecalho(ServiceContext.getCurrent(), 
/* 448 */               elemCabecalho);
/* 449 */           EntityPropertyDescriptor[] fds = barra.getState().getDao().getSQLProvider().getPkObjectUID()
/* 450 */             .getFieldDescriptors();
/* 451 */           Collection<EntityPrimaryKey> pksEnvolvidas = barra.getDadosBarramento().getPksEnvolvidas();
/* 452 */           EntityPrimaryKey cabKey = pksEnvolvidas.iterator().next();
/*     */           
/* 454 */           for (int i = 0; i < fds.length; i++) {
/* 455 */             EntityPropertyDescriptor cabEntity = fds[i];
/* 456 */             if ("NUNOTA".equals(cabEntity.getField().getName())) {
/* 457 */               nuNota = new BigDecimal(cabKey.getValues()[i].toString());
/*     */             }
/*     */           } 
/* 460 */           if (nuNota == null) {
/* 461 */             gravarMensagemErro(dwfEntityFacade, notaOrigemVO, "Nfoi possgerar a nota");
/*     */           }
/*     */           
/* 464 */           dwfEntityFacade.clearSessionCache("CabecalhoNota");
/*     */           
/* 466 */           cacHelper.addIncluirAlterarListener(
/* 467 */               (CACHelper.IncluirAlterarItemListener)new LoteAutomaticoHelper());
/*     */           
/* 469 */           JapeSession.putProperty("ItemNota.incluindo.alterando.pela.central", Boolean.TRUE);
/* 470 */           Element itensElem = new Element("itens");
/* 471 */           itensElem.setAttribute("ATUALIZACAO_ONLINE", "false");
/*     */           
/* 473 */           if (strNotas.length() > 0) {
/* 474 */             strNotas.append(", ");
/*     */           }
/*     */           
/* 477 */           strNotas.append(nuNota);
/* 478 */           DynamicVO prodItemVO = (DynamicVO)dwfEntityFacade.findEntityByPrimaryKeyAsVO("Servico", 
/* 479 */               new Object[] { configVO.asBigDecimal("CODPROD") });
/*     */           
/* 481 */           BigDecimal valorUnitario = 
/* 482 */             BigDecimalUtil.getRounded(
/* 483 */               vlrTotal.multiply(configVO.asBigDecimalOrZero("PERCTAXA"))
/* 484 */               .divide(BigDecimalUtil.CEM_VALUE, BigDecimalUtil.MATH_CTX), 
/* 485 */               prodItemVO.asInt("DECVLR"));
/*     */           
/* 487 */           Element itemElem = new Element("item");
/* 488 */           XMLUtils.addContentElement(itemElem, "NUNOTA", nuNota);
/* 489 */           XMLUtils.addContentElement(itemElem, "SEQUENCIA", "");
/* 490 */           XMLUtils.addContentElement(itemElem, "CODPROD", prodItemVO.asBigDecimal("CODPROD"));
/* 491 */           XMLUtils.addContentElement(itemElem, "CODVOL", prodItemVO.asString("CODVOL"));
/* 492 */           XMLUtils.addContentElement(itemElem, "QTDNEG", BigDecimal.ONE);
/* 493 */           XMLUtils.addContentElement(itemElem, "PERCDESC", BigDecimal.ZERO);
/* 494 */           XMLUtils.addContentElement(itemElem, "VLRDESC", BigDecimal.ZERO);
/* 495 */           XMLUtils.addContentElement(itemElem, "VLRUNIT", valorUnitario);
/* 496 */           XMLUtils.addContentElement(itemElem, "VLRTOT", valorUnitario);
/* 497 */           itensElem.addContent((Content)itemElem);
/*     */           
/* 499 */           BarramentoRegra.DadosBarramento dadosBarramento = cacHelper.incluirAlterarItem(nuNota, 
/* 500 */               ServiceContext.getCurrent(), itensElem, false);
/* 501 */           EntityPropertyDescriptor[] fdsItem = dwfEntityFacade.getDAOInstance("ItemNota").getSQLProvider()
/* 502 */             .getPkObjectUID().getFieldDescriptors();
/*     */           
/* 504 */           Collection<EntityPrimaryKey> pksEnvolvidasItem = dadosBarramento.getPksEnvolvidas();
/* 505 */           BigDecimal sequencia = null;
/* 506 */           if (pksEnvolvidasItem != null && pksEnvolvidasItem.size() > 0) {
/* 507 */             EntityPrimaryKey itemKey = pksEnvolvidasItem.iterator().next();
/* 508 */             for (int j = 0; j < fdsItem.length; j++) {
/* 509 */               EntityPropertyDescriptor itemEntity = fdsItem[j];
/* 510 */               if ("SEQUENCIA".equals(itemEntity.getField().getName())) {
/* 511 */                 sequencia = new BigDecimal(itemKey.getValues()[j].toString());
/*     */               }
/*     */             } 
/*     */           } 
/*     */           
/* 516 */           gravarDescontoCredito(parceiroVO.asBigDecimal("CODPARC"), configVO.asBigDecimal("CODEMPDEST"), 
/* 517 */               nuNota);
/* 518 */           Collection<Exception> erros = dadosBarramento.getErros();
/* 519 */           if (erros.size() > 0) {
/* 520 */             throw (Exception)erros.iterator().next();
/*     */           }
/*     */           
/* 523 */           Collection<LiberacaoSolicitada> liberacoes = dadosBarramento.getLiberacoesSolicitadas();
/*     */           
/* 525 */           if (liberacoes.size() > 0) {
/* 526 */             gravarMensagemErro(dwfEntityFacade, notaOrigemVO, 
/* 527 */                 "Nfoi possgerar a nota. Solicitade liberagerada: " + (
/* 528 */                 (LiberacaoSolicitada)liberacoes.iterator().next()).getDescricao());
/*     */           }
/*     */           
/* 531 */           Collection<ClientEvent> clientEvents = dadosBarramento.getClientEvents();
/*     */           
/* 533 */           if (clientEvents.size() > 0) {
/* 534 */             gravarMensagemErro(dwfEntityFacade, notaOrigemVO, "Nfoi possgerar a nota. Evento solicitado: " + (
/* 535 */                 (ClientEvent)clientEvents.iterator().next()).getEventID());
/*     */           }
/*     */           
/* 538 */           if (sequencia == null) {
/* 539 */             gravarMensagemErro(dwfEntityFacade, notaOrigemVO, "Nfoi possgerar a nota. Nenhum item gerado.");
/*     */           }
/*     */           
/* 542 */           notaOrigemVO.setProperty("AD_STATUSROYAL", "1");
					notaOrigemVO.setProperty("AD_ERROROYALTIES", erroAoCalcular);
/* 543 */           notaPersistent.setValueObject((EntityVO)notaOrigemVO);
/*     */         } 
					try {
						if("N".equals(erroAoCalcular)) {
							validaRoyalties.atualizaItens(notaOrigemVO.asBigDecimal("NUNOTA"));
						}
					} catch(Exception e) {
						gravarMensagemErro(dwfEntityFacade, notaOrigemVO, "Falha ao atualizar itens: " + e.getMessage());
					}
/* 545 */         gravarMensagemSucesso(dwfEntityFacade, notaOrigemVO, strNotas);
				} catch(Exception e) {
					try {
						gravarMensagemErro(dwfEntityFacade, cabVO, "Falha ao atualizar itens: " + e.getMessage());
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
/*     */       } 
				
/* 547 */       try {
	rsGerarRoyalties.close();
} catch (SQLException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}
/* 548 */     //} catch (Exception e) {
	           // try {
			//		gravarMensagemErro(dwfEntityFacade, cabVO, "Erro ao gerar Royalties. " + e.getMessage());
				//} catch (Exception e1) {
					// TODO Auto-generated catch block
				//	e1.printStackTrace();
				//}
/* 549 */       //System.out.println("Falha ao atualizar dados: " + e.getMessage());
/* 550 */       //e.printStackTrace();
/*     */     //} 
			} catch(Exception e) {
				e.printStackTrace();
			}
/*     */   }
/*     */ 
/*     */   
/*     */   private void gravarMensagemErro(EntityFacade dwfEntityFacade, DynamicVO notaOrigemVO, String mensagem) throws Exception {
/* 556 */     DynamicVO mensagemVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("AD_MSGATUALROYALTIES");
			  JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
/* 557 */     mensagemVO.setProperty("NUNOTA", notaOrigemVO.asBigDecimal("NUNOTA"));
/* 558 */     mensagemVO.setProperty("MENSAGEM", 
/* 559 */         (StringUtils.getNullAsEmpty(mensagem).length() > 3999) ? mensagem.substring(0, 3999) : mensagem);
/* 560 */     mensagemVO.setProperty("DHMENSAGEM", TimeUtils.getNow());
/* 561 */     mensagemVO.setProperty("TIPO", String.valueOf("E"));
/* 562 */     dwfEntityFacade.createEntity("AD_MSGATUALROYALTIES", (EntityVO)mensagemVO);
			  PersistentLocalEntity notaPersistent = dwfEntityFacade.findEntityByPrimaryKey("CabecalhoNota", notaOrigemVO.asBigDecimal("NUNOTA"));
			  notaOrigemVO.setProperty("AD_ERROROYALTIES", "S");
/* 543 */     notaPersistent.setValueObject((EntityVO)notaOrigemVO);


				erroAoCalcular = "S";
				
				cabDAO.deleteByCriteria("AD_NUNOTAORIG = ?", notaOrigemVO.asBigDecimal("NUNOTA"));
				
/*     */   }
/*     */ 
/*     */   
/*     */   private void gravarMensagemSucesso(EntityFacade dwfEntityFacade, DynamicVO notaOrigemVO, StringBuffer strNotas) throws Exception {
/* 567 */     DynamicVO mensagemVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("AD_MSGATUALROYALTIES");
/* 568 */     mensagemVO.setProperty("NUNOTA", notaOrigemVO.asBigDecimal("NUNOTA"));
/* 569 */     mensagemVO.setProperty("MENSAGEM", 
/* 570 */         String.format("Pedidos %s gerados com sucesso.", new Object[] { strNotas.toString() }));
/* 571 */     mensagemVO.setProperty("DHMENSAGEM", TimeUtils.getNow());
/* 572 */     mensagemVO.setProperty("TIPO", String.valueOf("I"));
/* 573 */     dwfEntityFacade.createEntity("AD_MSGATUALROYALTIES", (EntityVO)mensagemVO);
/*     */   }
/*     */   
/*     */   private void setupContext() {
/* 577 */     AuthenticationInfo auth = AuthenticationInfo.getCurrent();
/* 578 */     JapeSessionContext.putProperty("usuario_logado", auth.getUserID());
/* 579 */     JapeSessionContext.putProperty("authInfo", auth);
/* 580 */     JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
/* 581 */     JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
/* 582 */     JapeSession.putProperty("ItemNota.incluindo.alterando.pela.central", Boolean.TRUE);
/*     */   }
/*     */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\BotaoGerarRoyalties.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */