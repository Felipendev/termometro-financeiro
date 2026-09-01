# Termômetro Financeiro — front-end

Dashboard dos Três Eus (fatia 13, RN-11). React + Vite + TypeScript, consumindo o único endpoint
agregado `GET /v1/dashboard/tres-eus?competencia=`. Sem edição — v1 é só leitura.

## Rodando

```bash
npm install
cp .env.example .env.local   # ajuste VITE_API_BASE_URL se a API não estiver em localhost:8080
npm run dev
```

Precisa do backend Spring Boot rodando (porta 8080 por padrão) com CORS liberado para a origem do
Vite (`http://localhost:5173` por padrão — ver `app.cors.allowed-origins` no `application.yml` do
backend).

## Precondição

Classificação, triagem, não-gasto e compromissos futuros precisam ter rodado para a competência
que você está olhando — o dashboard só lê, não dispara nenhum desses passos. Se o mês não foi
processado ainda, a coluna "Eu do Presente" aparece com a triagem zerada — não é bug.

## Fora do escopo

Simulador de compra (RN-11), qualquer edição pela UI (metas, decisão sobre vampiro, promoção de
transação a vermelha — tudo isso já existe na API, mas não tem tela aqui), autenticação.
