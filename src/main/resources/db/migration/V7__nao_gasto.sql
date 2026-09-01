-- RN-03: transações que não são gasto (pagamento de fatura, transferência própria, estorno) não
-- entram em nenhum agregado. Default false preserva o comportamento de todo o histórico já
-- importado até que o motor de conciliação rode sobre ele.
alter table transacao add column ignorada boolean not null default false;
