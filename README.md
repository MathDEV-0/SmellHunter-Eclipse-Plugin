# Plugin de Análise de Métricas de Software

## 1. Introdução

Este é um plugin para a plataforma Eclipse IDE projetado para analisar e visualizar métricas de qualidade de software. A ferramenta oferece uma interface de usuário interativa para carregar os arquivos necessários, validar a seleção e exibir os dados de forma clara e objetiva, tanto em formato de tabela quanto em um gráfico de barras.

O objetivo principal é fornecer feedback visual imediato sobre a qualidade de um sistema ou classe com base em um conjunto de métricas predefinidas, como LOC, CBO, WMC, entre outras.

## 2. Funcionalidades

* **Interface de Usuário Moderna e Interativa:** Painel dedicado para gerenciamento de arquivos com uma aparência limpa e organizada.
* **Carregamento de Arquivos Guiado:** Slots específicos para cada tipo de arquivo necessário:
    * 1 arquivo de domínio (`.smelldsl`)
    * 2 arquivos de métricas (`.csv` ou `.json`)
* **Validação em Tempo Real:**
    * Indicadores de status coloridos (Vermelho/Verde) mostram instantaneamente quais arquivos ainda precisam ser carregados.
    * O botão de processamento só é habilitado após todos os arquivos necessários serem anexados.
* **Gerenciamento de Arquivos:** Permite anexar e remover arquivos individualmente antes do processamento.
* **Visualização Dupla dos Dados:**
    * **Tabela de Métricas:** Exibe o valor bruto de cada métrica e sua classificação qualitativa (LOW, MEDIUM, HIGH).
    * **Gráfico de Barras:** Apresenta uma visualização gráfica da classificação de cada métrica, facilitando a identificação de pontos críticos.
* **Customização Visual:** O tema do gráfico e os componentes da UI foram ajustados para uma aparência mais moderna, garantindo melhor integração com temas de IDE escuros e claros.

## 3. Como Usar

1.  **Abra a View do Plugin:** No seu ambiente Eclipse, navegue até `Window` > `Show View` > `Other...` e selecione a view do plugin (por exemplo, "MyView").
2.  **Anexe os Arquivos:**
    * Na seção "Gerenciamento de Arquivos", você verá três slots.
    * Clique no botão **"Anexar..."** de cada slot para selecionar o arquivo correspondente.
    * O indicador de status mudará de vermelho para verde assim que um arquivo for anexado.
3.  **Exclua (se necessário):** Se você selecionou um arquivo incorreto, clique no botão **"Excluir"** ao lado dele para limpar o slot.
4.  **Processe os Dados:**
    * Após anexar os três arquivos, o botão **"Confirmar e Processar"** será habilitado.
    * Clique nele para iniciar a análise.
5.  **Analise os Resultados:**
    * Os dados do primeiro arquivo de métricas serão exibidos na tabela e no gráfico abaixo.
    * A tabela mostrará os valores numéricos e a classificação.
    * O gráfico de barras representará visualmente essas classificações.

## 4. Detalhes Técnicos

* **Plataforma:** Eclipse RCP (Rich Client Platform)
* **Linguagem:** Java
* **UI Toolkit:** SWT (Standard Widget Toolkit)
* **Biblioteca de Gráficos:** SWTChart

---
