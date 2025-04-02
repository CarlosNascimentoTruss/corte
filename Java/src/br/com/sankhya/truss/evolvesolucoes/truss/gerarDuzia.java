/*     */ package br.com.evolvesolucoes.truss;
/*     */ import java.math.BigDecimal;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collection;

/*     */ import com.sankhya.util.BigDecimalUtil;

/*     */ 
/*     */ import br.com.sankhya.commons.xml.Element;
/*     */ import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
/*     */ import br.com.sankhya.extensions.actionbutton.ContextoAcao;
/*     */ import br.com.sankhya.extensions.actionbutton.Registro;
/*     */ import br.com.sankhya.jape.EntityFacade;
/*     */ import br.com.sankhya.jape.bmp.PersistentLocalEntity;
/*     */ import br.com.sankhya.jape.core.JapeSession;
/*     */ import br.com.sankhya.jape.dao.JdbcWrapper;
/*     */ import br.com.sankhya.jape.util.FinderWrapper;
/*     */ import br.com.sankhya.jape.vo.DynamicVO;
/*     */ import br.com.sankhya.jape.vo.EntityVO;
/*     */ import br.com.sankhya.jape.vo.PrePersistEntityState;
/*     */ import br.com.sankhya.modelcore.auth.AuthenticationInfo;
/*     */ import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
/*     */ import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*     */ import br.com.sankhya.modelcore.util.SPBeanUtils;
/*     */ import br.com.sankhya.ws.ServiceContext;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class gerarDuzia
/*     */   implements AcaoRotinaJava
/*     */ {
/*     */   public void doAction(ContextoAcao contexto) throws Exception {
/*  36 */     JapeSession.SessionHandle hnd = null;
/*  37 */     JdbcWrapper jdbc = null;
/*     */     
/*  39 */     BigDecimal nunotaBonificacao = new BigDecimal(0);
/*     */     
/*  41 */     Registro[] lancamentos = contexto.getLinhas();
/*     */     
/*  43 */     Boolean processaBonificacao = Boolean.TRUE;
/*     */     
/*  45 */     processaBonificacao = Boolean.valueOf(contexto.confirmarSimNao("Bonificação", 
/*  46 */           "Processamento de Produtos bonificadaos<br> Deseja continuar?", 1));
/*     */     
/*  48 */     if (processaBonificacao.booleanValue()) {
/*     */       byte b; int i; Registro[] arrayOfRegistro;
/*  50 */       for (i = (arrayOfRegistro = lancamentos).length, b = 0; b < i; ) { Registro registro = arrayOfRegistro[b];
/*     */         
/*  52 */         hnd = JapeSession.open();
/*  53 */         EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/*  54 */         jdbc = dwfEntityFacade.getJdbcWrapper();
/*     */         
/*  56 */         Collection<DynamicVO> produto = dwfEntityFacade
/*  57 */           .findByDynamicFinderAsVO(new FinderWrapper("AD_TITENSBONPROM", "this.SEQ = ?", 
/*  58 */               new Object[] { contexto.getLinhas()[0].getCampo("SEQ") }));
/*     */         
/*  60 */         for (DynamicVO produtoVO : produto) {
/*     */           
/*  62 */           if (registro.getCampo("NUNOTA") != null) {
/*  63 */             contexto.mostraErro("Pedido ja gerado anteriormente!!!");
/*     */           }
/*     */           
/*  66 */           BigDecimal promocao = (BigDecimal)produtoVO.getProperty("CODPROM");
/*     */           
/*  68 */           CACHelper cacHelper = new CACHelper();
/*  69 */           ServiceContext ctx = new ServiceContext(null);
/*     */           
/*  71 */           AuthenticationInfo authInfo = new AuthenticationInfo("SUP ", BigDecimalUtil.ZERO_VALUE, 
/*  72 */               BigDecimalUtil.ZERO_VALUE, Integer.valueOf(0));
/*  73 */           authInfo.makeCurrent();
/*  74 */           Element requestBody = new Element("requestBody");
/*  75 */           ctx.setRequestBody((Element)requestBody);
/*  76 */           ctx.setAutentication(authInfo);
/*  77 */           ctx.makeCurrent();
/*     */           
/*  79 */           SPBeanUtils.setupContext(ctx);
/*     */           
/*  81 */           DynamicVO pedidoVO = (DynamicVO)dwfEntityFacade
/*  82 */             .findEntityByPrimaryKeyAsVO("CabecalhoNota", registro.getCampo("NUNOTA"));
/*     */           
/*  84 */           DynamicVO empresaFinVO = (DynamicVO)dwfEntityFacade
/*  85 */             .findEntityByPrimaryKeyAsVO("EmpresaFinanceiro", registro.getCampo("CODEMP"));
/*     */           
/*  87 */           BigDecimal codCenCus = (BigDecimal)empresaFinVO.getProperty("AD_CODCENCUSBON");
/*  88 */           BigDecimal codNat = (BigDecimal)empresaFinVO.getProperty("AD_CODNATBON");
/*  89 */           BigDecimal codParc = (BigDecimal)registro.getCampo("CODPARC");
/*  90 */           BigDecimal codTipoPer = (BigDecimal)pedidoVO.getProperty("CODTIPVENDA");
/*  91 */           BigDecimal codtipvenda = BigDecimal.ONE;
/*  92 */           BigDecimal codEmp = (BigDecimal)registro.getCampo("CODEMP");
/*     */           
/*  94 */           PersistentLocalEntity cabEntity = dwfEntityFacade.findEntityByPrimaryKey("CabecalhoNota", new Object[] { produtoVO.getProperty("NUNOTA") });
/*  95 */           DynamicVO cabVO = (DynamicVO)cabEntity.getValueObject();
/*     */           
/*  97 */           BigDecimal vlrPedido = (BigDecimal)cabVO.getProperty("VLRNOTA");
/*  98 */           cabVO.setProperty("AD_NUNOTA", produtoVO.getProperty("NUNOTA"));
/*  99 */           cabEntity.setValueObject((EntityVO)cabVO);
/*     */           
/* 101 */           nunotaBonificacao = criaCabecalho(contexto, codEmp, codEmp, codCenCus, codNat, codParc, codTipoPer, 
/* 102 */               codtipvenda, (BigDecimal)produtoVO.getProperty("NUNOTA"), vlrPedido);
/*     */ 
/*     */ 
/*     */           
/* 106 */           Collection<DynamicVO> itens = dwfEntityFacade
/* 107 */             .findByDynamicFinderAsVO(new FinderWrapper("AD_TITENSBONGANHE", 
/* 108 */                 "this.SEQ = " + contexto.getLinhas()[0].getCampo("SEQ") + " and this.CODPROM = ? ", 
/* 109 */                 new Object[] { promocao }));
/*     */           
/* 111 */           for (DynamicVO itensVO : itens) {
/*     */             
/* 113 */             EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
/* 114 */             Collection<PrePersistEntityState> itensNota = new ArrayList<>();
/*     */             
/* 116 */             DynamicVO itemRoyaltiesVO = (DynamicVO)dwfEntityFacade
/* 117 */               .getDefaultValueObjectInstance("ItemNota");
/* 118 */             itemRoyaltiesVO.setPrimaryKey(null);
/* 119 */             itemRoyaltiesVO.setProperty("NUNOTA", nunotaBonificacao);
/* 120 */             itemRoyaltiesVO.setProperty("SEQUENCIA", itensVO.getProperty("SEQG"));
/* 121 */             itemRoyaltiesVO.setProperty("CODPROD", itensVO.getProperty("CODPROD"));
/* 122 */             itemRoyaltiesVO.setProperty("CONTROLE", " ");
/* 123 */             itemRoyaltiesVO.setProperty("CODLOCALORIG", empresaFinVO.getProperty("LOCALPAD"));
/* 124 */             itemRoyaltiesVO.setProperty("QTDNEG", itensVO.getProperty("QTDGANHE"));
/* 125 */             itemRoyaltiesVO.setProperty("QTDENTREGUE", BigDecimal.ZERO);
/* 126 */             itemRoyaltiesVO.setProperty("VLRUNIT", BigDecimal.ONE);
/* 127 */             itemRoyaltiesVO.setProperty("VLRDESC", BigDecimal.ZERO);
/* 128 */             itemRoyaltiesVO.setProperty("VLRTOT", BigDecimal.ONE);
/*     */             
/* 130 */             PrePersistEntityState itemMontado = PrePersistEntityState.build(dwfFacade, 
/* 131 */                 "ItemNota", itemRoyaltiesVO);
/*     */             
/* 133 */             itensNota.add(itemMontado);
/*     */             
/* 135 */             cacHelper.incluirAlterarItem(new BigDecimal(nunotaBonificacao.toString()), ctx, itensNota, 
/* 136 */                 true);
/*     */           } 
/*     */         } 
/*     */         b++; }
/*     */     
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public BigDecimal criaCabecalho(ContextoAcao contexto, BigDecimal codEmp, Object codEmpNegoc, BigDecimal codCenCus, BigDecimal codNat, BigDecimal parceiro, BigDecimal codTipoPer, BigDecimal codtipvenda, BigDecimal nuOrigem, BigDecimal vlrPedido) throws Exception {
/*     */     try {
/* 150 */       Registro cabecalho = contexto.novaLinha("TGFCAB");
/* 151 */       cabecalho.setCampo("CODEMP", codEmp);
/* 152 */       cabecalho.setCampo("CODEMPNEGOC", codEmpNegoc);
/* 153 */       cabecalho.setCampo("CODCENCUS", codCenCus);
/* 154 */       cabecalho.setCampo("CODNAT", codNat);
/* 155 */       cabecalho.setCampo("CODPARC", parceiro);
/* 156 */       cabecalho.setCampo("CODTIPOPER", codTipoPer);
/* 157 */       cabecalho.setCampo("CODTIPVENDA", codtipvenda);
/* 158 */       cabecalho.setCampo("NUMNOTA", Integer.valueOf(0));
/* 159 */       cabecalho.setCampo("CIF_FOB", String.valueOf("S"));
/* 160 */       cabecalho.setCampo("AD_NUNOTA", nuOrigem);
/* 161 */       cabecalho.setCampo("AD_VLRPED", vlrPedido);
/* 162 */       cabecalho.save();
/*     */       
/* 164 */       return (BigDecimal)cabecalho.getCampo("NUNOTA");
/*     */     }
/* 166 */     catch (Exception e) {
/* 167 */       e.printStackTrace();
/* 168 */       contexto.setMensagemRetorno(e.getMessage());
/*     */       
/* 170 */       return new BigDecimal(0);
/*     */     } 
/*     */   }
/*     */   private String getLinkMov(String descricao, String chave) {
/* 174 */     String url = "<a title=\"Baixar Arquivo\" href=\"/mge/visualizadorArquivos.mge?chaveArquivo={0}\" target=\"_blank\"><u><b>{1}</b></u></a>"
/* 175 */       .replace("{0}", chave);
/* 176 */     url = url.replace("{1}", descricao);
/* 177 */     return url;
/*     */   }
/*     */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\gerarDuzia.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */