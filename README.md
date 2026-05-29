## 3 增强优化环节（全篇核心）
---

## 3.1 预检索增强
### 3.1.1 输入增强
- 元数据提取（NER）
- 语义提取（语义层次图）

### 3.1.2 查询增强
- 查询重写
  - RaFe
  - DMQR-RAG
  - RQ-RAG
  - EVO-RAG
- 语义扩展
  - 回译扩展
  - QOQA
- 主题过滤与迭代
  - AT-RAG

---

## 3.2 检索器增强
### 3.2.1 检索器
- 稀疏检索
  - BM25
  - TF-IDF
- 稠密检索
  - DPR
  - ColBERT
  - Contriever
  - BGE 系列
  - text-embedding-ada-002
- 生成式检索
  - ListGR
  - PAG
  - MVDR-GR

### 3.2.2 检索器与LLM对齐
- 微调检索器
  - AAR
  - ARL2
  - ToolLLM
  - DocReLM
- 适配器引入
  - PRCA
  - Oreo
- 偏好对齐
  - DPA-RAG
  - LarPO

---

## 3.3 检索策略增强
### 3.3.1 多步优化策略
- 迭代检索
  - Auto-RAG
  - AT-RAG
  - IUM
- 递归检索
  - AirRAG
  - HIRO
  - SiReRAG
  - RAG-Star

### 3.3.2 动态调整策略
- 置信度判断
  - CtrlA
  - WeKnow-RAG
  - Vendi-RAG
- 生成内容评估
  - Self-RAG
  - Open-RAG
  - DeepRAG
  - Sufficient Context RAG
- 任务复杂度自适应
  - Adaptive-RAG
  - MBA-RAG

### 3.3.3 上下文增强策略
- 混合检索
  - Blended RAG
  - Hybrid RAG
  - Ask-EDA
- 上下文扩展检索
  - ConvRAG
  - CAG
  - ADACQR
  - Ms. WoW

---

## 3.4 索引增强
### 3.4.1 数据源增强
- 非结构化数据优化
  - RAGtrans
  - ISAG
- 结构化数据优化
  - Graph RAG
  - KET-RAG
  - CABINET
  - DoTTeR
  - ToG
  - ToG 2.0
  - HippoRAG / HippoRAG2

### 3.4.2 索引结构优化
- 分布式索引
  - Milvus
  - TELERAG
- 分层索引
  - RAPTOR
  - SiReRAG
  - KG-Retriever

---

## 3.5 检索后增强
### 3.5.1 重排序与过滤
- 基于规则重排序
- 基于模型重排序
  - FiD-Light
  - W-RAG
  - RADIO
  - CFT-RAG
  - G-RAG
  - RankRAG
  - RankCoT
  - ASRank

### 3.5.2 信息压缩
- MAC
- Refiner
- COCOM
- AMR 概念蒸馏

---

## 3.6 大语言模型增强
### 3.6.1 预训练增强
- REALM
- Atlas
- UniGen

### 3.6.2 微调增强
- RoG
- RAFT
- RA-DIT
- Reward-RAG
- JMLR
- IM-RAG
- RAG-DDR

### 3.6.3 推理增强
- 提示工程
  - FLARE
  - RAT
- Agentic RAG
  - ReAct
  - RTLFixer
  - PlanRAG
  - AutoAgent
  - Search-o1
  - RAG-Gym
- 工作流智能体
  - DepsRAG
  - MAIN-RAG
  - MMOA-RAG
  - CoA
