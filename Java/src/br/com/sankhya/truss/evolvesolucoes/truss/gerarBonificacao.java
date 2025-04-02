/*     */ package br.com.sankhya.truss.evolvesolucoes.truss;
/*     */ import java.math.BigDecimal;
/*     */ import java.sql.ResultSet;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collection;

/*     */ import com.sankhya.util.BigDecimalUtil;

/*     */ 
/*     */ import br.com.sankhya.commons.xml.Element;
/*     */ import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
/*     */ import br.com.sankhya.extensions.actionbutton.ContextoAcao;
/*     */ import br.com.sankhya.extensions.actionbutton.Registro;
/*     */ import br.com.sankhya.jape.EntityFacade;
/*     */ import br.com.sankhya.jape.core.JapeSession;
/*     */ import br.com.sankhya.jape.dao.JdbcWrapper;
/*     */ import br.com.sankhya.jape.sql.NativeSql;
/*     */ import br.com.sankhya.jape.vo.DynamicVO;
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
/*     */ public class gerarBonificacao
/*     */   implements AcaoRotinaJava
/*     */ {
/*     */   public void doAction(ContextoAcao contexto) throws Exception {
/*  33 */     JapeSession.SessionHandle hnd = null;
/*  34 */     JdbcWrapper jdbc = null;
/*     */     
/*  36 */     BigDecimal nunotaBonificacao = new BigDecimal(0);
/*     */     
/*  38 */     Registro[] lancamentos = contexto.getLinhas();
/*     */ 
/*     */     
/*  41 */     Boolean processaBonificacao = Boolean.TRUE;
/*     */     
/*  43 */     processaBonificacao = Boolean.valueOf(contexto.confirmarSimNao("Bonificação", 
/*  44 */           "Processamento de Produtos bonificadaos<br> Deseja continuar?", 1));
/*     */     
/*  46 */     if (processaBonificacao.booleanValue()) {
/*     */       byte b; int i; Registro[] arrayOfRegistro;
/*  48 */       for (i = (arrayOfRegistro = lancamentos).length, b = 0; b < i; ) { Registro registro = arrayOfRegistro[b];
/*     */ 
/*     */         
/*     */         try {
/*  52 */           if (registro.getCampo("NUNOTA") != null) {
/*  53 */             contexto.mostraErro("Pedido ja gerado anteriormente!!!");
/*     */           }
/*     */           
/*  56 */           hnd = JapeSession.open();
/*  57 */           EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/*  58 */           jdbc = dwfEntityFacade.getJdbcWrapper();
/*     */           
/*  60 */           CACHelper cacHelper = new CACHelper();
/*  61 */           ServiceContext ctx = new ServiceContext(null);
/*     */           
/*  63 */           AuthenticationInfo authInfo = new AuthenticationInfo("SUP ", BigDecimalUtil.ZERO_VALUE, 
/*  64 */               BigDecimalUtil.ZERO_VALUE, Integer.valueOf(0));
/*  65 */           authInfo.makeCurrent();
/*  66 */           Element requestBody = new Element("requestBody");
/*  67 */           ctx.setRequestBody((Element)requestBody);
/*  68 */           ctx.setAutentication(authInfo);
/*  69 */           ctx.makeCurrent();
/*     */           
/*  71 */           SPBeanUtils.setupContext(ctx);
/*     */           
/*  73 */           if ((contexto.getLinhas()).length == 0) {
/*  74 */             throw new Exception("Selecione um registro");
/*     */           }
/*     */           
/*  77 */           if ((contexto.getLinhas()).length > 1) {
/*  78 */             throw new Exception("Selecione apenas um registro");
/*     */           }
/*     */           
/*  81 */           DynamicVO empresaFinVO = (DynamicVO)dwfEntityFacade
/*  82 */             .findEntityByPrimaryKeyAsVO("EmpresaFinanceiro", registro.getCampo("CODEMP"));
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */           
/*  91 */           if (empresaFinVO.getProperty("AD_CODTIPOPERBON") == null) {
/*  92 */             contexto.mostraErro("Cadastrar para empresa " + empresaFinVO.getProperty("CODEMP") + 
/*  93 */                 " o campo Cód.Top Bonificação, dentro do menu Comercial » Preferências » Empresa Aba Bonificação");
/*     */           }
/*     */ 
/*     */           
/*  97 */           if (empresaFinVO.getProperty("AD_CODCENCUSBON") == null) {
/*  98 */             contexto.mostraErro("Cadastrar para empresa " + empresaFinVO.getProperty("CODEMP") + 
/*  99 */                 " o campo Cód. CR Bonificação, dentro do menu Comercial » Preferências » Empresa Aba Bonificação");
/*     */           }
/*     */ 
/*     */           
/* 103 */           if (empresaFinVO.getProperty("AD_CODNATBON") == null) {
/* 104 */             contexto.mostraErro("Cadastrar para empresa " + empresaFinVO.getProperty("CODEMP") + 
/* 105 */                 " o campo Cód. Nat Bonificação, dentro do menu Comercial » Preferências » Empresa Aba Bonificação");
/*     */           }
/*     */ 
/*     */           
/* 109 */           BigDecimal codCenCus = (BigDecimal)empresaFinVO.getProperty("AD_CODCENCUSBON");
/* 110 */           BigDecimal codNat = (BigDecimal)empresaFinVO.getProperty("AD_CODNATBON");
/* 111 */           BigDecimal codParc = (BigDecimal)registro.getCampo("CODPARC");
/* 112 */           BigDecimal codTipoPer = (BigDecimal)empresaFinVO.getProperty("AD_CODTIPOPERBON");
/* 113 */           BigDecimal codtipvenda = BigDecimal.ONE;
/* 114 */           BigDecimal codEmp = (BigDecimal)registro.getCampo("CODEMP");
/*     */           
/* 116 */           nunotaBonificacao = criaCabecalho(contexto, codEmp, codEmp, codCenCus, codNat, codParc, codTipoPer, 
/* 117 */               codtipvenda, (BigDecimal)registro.getCampo("NUNOTA"));
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */           
/* 125 */           NativeSql qryItensBonificados = new NativeSql(jdbc);
/*     */           
/* 127 */           qryItensBonificados.appendSql(
/* 128 */               "SELECT ROWNUM LINHAS,G.SEQ,G.CODPROM,G.SEQG,G.REFERENCIA,G.QTDGANHE,G.SELECIONE, G.CODPROD FROM AD_TITENSBONGANHE G WHERE G.SEQ= " + 
/* 129 */               registro.getCampo("SEQ"));
/*     */           
/* 131 */           ResultSet rsProdutos = qryItensBonificados.executeQuery();
/*     */           
/* 133 */           if (rsProdutos.next()) {
/*     */             
/* 135 */             DynamicVO itemRoyaltiesVO = (DynamicVO)dwfEntityFacade
/* 136 */               .getDefaultValueObjectInstance("ItemNota");
/* 137 */             itemRoyaltiesVO.setPrimaryKey(null);
/* 138 */             itemRoyaltiesVO.setProperty("NUNOTA", nunotaBonificacao);
/* 139 */             itemRoyaltiesVO.setProperty("SEQUENCIA", rsProdutos.getBigDecimal("LINHAS"));
/* 140 */             itemRoyaltiesVO.setProperty("CODPROD", rsProdutos.getBigDecimal("CODPROD"));
/* 141 */             itemRoyaltiesVO.setProperty("CONTROLE", " ");
/* 142 */             itemRoyaltiesVO.setProperty("CODLOCALORIG", empresaFinVO.getProperty("LOCALPAD"));
/* 143 */             itemRoyaltiesVO.setProperty("QTDNEG", rsProdutos.getBigDecimal("QTDGANHE"));
/* 144 */             itemRoyaltiesVO.setProperty("QTDENTREGUE", BigDecimal.ZERO);
/* 145 */             itemRoyaltiesVO.setProperty("VLRUNIT", BigDecimal.ONE);
/* 146 */             itemRoyaltiesVO.setProperty("VLRDESC", BigDecimal.ZERO);
/* 147 */             itemRoyaltiesVO.setProperty("VLRTOT", BigDecimal.ONE);
/*     */             
/* 149 */             Collection<PrePersistEntityState> itensNota = new ArrayList<>();
/* 150 */             PrePersistEntityState itemMontado = PrePersistEntityState.build(dwfEntityFacade, 
/* 151 */                 "ItemNota", itemRoyaltiesVO);
/* 152 */             itensNota.add(itemMontado);
/* 153 */             cacHelper.incluirAlterarItem(new BigDecimal(nunotaBonificacao.toString()), ctx, itensNota, 
/* 154 */                 true);
/*     */           } 
/* 156 */           registro.setCampo("NUNOTA", nunotaBonificacao);
/* 157 */           rsProdutos.close();
/*     */           
/* 159 */           StringBuilder msg = new StringBuilder();
/* 160 */           msg.append("Pedido de bonificação gerado com sucesso: ");
/* 161 */           msg.append(nunotaBonificacao);
/* 162 */           msg.append("! <br /><br />");
/* 163 */           msg.append(
/* 164 */               "<a href=\"javascript:workspace.openAppActivity('br.com.sankhya.com.mov.CentralNotas', {'NUNOTA': ");
/* 165 */           msg.append(nunotaBonificacao);
/* 166 */           msg.append("})\">");
/* 167 */           msg.append("Clique aqui para abrir");
/* 168 */           msg.append("</a><br /><br />");
/* 169 */           contexto.setMensagemRetorno(msg.toString());
/*     */           
/* 171 */           System.out.println("LINK: " + msg.toString());
/*     */         } finally {
/*     */           
/* 174 */           JapeSession.close(hnd);
/*     */         } 
/*     */         b++; }
/*     */     
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public BigDecimal criaCabecalho(ContextoAcao contexto, BigDecimal codEmp, Object codEmpNegoc, BigDecimal codCenCus, BigDecimal codNat, BigDecimal parceiro, BigDecimal codTipoPer, BigDecimal codtipvenda, BigDecimal nuOrigem) throws Exception {
/*     */     try {
/* 186 */       Registro cabecalho = contexto.novaLinha("TGFCAB");
/* 187 */       cabecalho.setCampo("CODEMP", codEmp);
/* 188 */       cabecalho.setCampo("CODEMPNEGOC", codEmpNegoc);
/* 189 */       cabecalho.setCampo("CODCENCUS", codCenCus);
/* 190 */       cabecalho.setCampo("CODNAT", codNat);
/* 191 */       cabecalho.setCampo("CODPARC", parceiro);
/* 192 */       cabecalho.setCampo("CODTIPOPER", codTipoPer);
/* 193 */       cabecalho.setCampo("CODTIPVENDA", codtipvenda);
/* 194 */       cabecalho.setCampo("NUMNOTA", Integer.valueOf(0));
/* 195 */       cabecalho.setCampo("CIF_FOB", String.valueOf("S"));
/* 196 */       cabecalho.setCampo("AD_NUNOTA", nuOrigem);
/* 197 */       cabecalho.save();
/*     */       
/* 199 */       return (BigDecimal)cabecalho.getCampo("NUNOTA");
/*     */     }
/* 201 */     catch (Exception e) {
/* 202 */       e.printStackTrace();
/* 203 */       contexto.setMensagemRetorno(e.getMessage());
/*     */       
/* 205 */       return new BigDecimal(0);
/*     */     } 
/*     */   }
/*     */   private String getLinkMov(String descricao, String chave) {
/* 209 */     String url = "<a title=\"Baixar Arquivo\" href=\"/mge/visualizadorArquivos.mge?chaveArquivo={0}\" target=\"_blank\"><u><b>{1}</b></u></a>"
/* 210 */       .replace("{0}", chave);
/* 211 */     url = url.replace("{1}", descricao);
/* 212 */     return url;
/*     */   }
/*     */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\gerarBonificacao.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */