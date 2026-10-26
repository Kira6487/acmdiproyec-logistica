-- Consultas de solo lectura para validar la migración de datos iniciales.

select
    p.codigo,
    p.nombre,
    p.stock_minimo,
    i.stock as stock_actual,
    um.codigo as unidad_medida
from public.productos p
join public.inventario i on i.id_producto = p.id_producto
join public.unidades_medida um on um.id_unidad_medida = p.id_unidad_medida
where p.codigo in (
    'DADO-2CM', 'DADO-2.5CM', 'DADO-3CM', 'DADO-4CM', 'DADO-5CM',
    'DADO-7CM', 'DADO-7.5CM', 'ESC-15CM', 'ESC-20CM', 'ESC-25CM', 'ESC-30CM'
)
order by p.codigo;

select
    m.codigo,
    m.fecha_movimiento,
    m.tipo,
    m.estado,
    mm.nombre as motivo,
    count(md.id_movimiento_detalle) as cantidad_detalles,
    sum(md.cantidad) as cantidad_total
from public.movimientos m
join public.motivos_movimiento mm
  on mm.id_motivo_movimiento = m.id_motivo_movimiento
join public.movimiento_detalle md on md.id_movimiento = m.id_movimiento
where m.codigo = 'CARGA-INICIAL-2026'
group by m.codigo, m.fecha_movimiento, m.tipo, m.estado, mm.nombre;

with saldo_calculado as (
    select
        p.id_producto,
        p.codigo,
        coalesce(sum(
            case
                when m.estado = 'VIGENTE' and m.tipo = 'ENTRADA' then md.cantidad
                when m.estado = 'VIGENTE' and m.tipo = 'SALIDA' then -md.cantidad
                else 0
            end
        ), 0) as stock_segun_movimientos
    from public.productos p
    left join public.movimiento_detalle md on md.id_producto = p.id_producto and md.activo
    left join public.movimientos m on m.id_movimiento = md.id_movimiento and m.activo
    where p.codigo in (
        'DADO-2CM', 'DADO-2.5CM', 'DADO-3CM', 'DADO-4CM', 'DADO-5CM',
        'DADO-7CM', 'DADO-7.5CM', 'ESC-15CM', 'ESC-20CM', 'ESC-25CM', 'ESC-30CM'
    )
    group by p.id_producto, p.codigo
)
select
    sc.codigo,
    i.stock as stock_actual,
    sc.stock_segun_movimientos,
    i.stock = sc.stock_segun_movimientos as saldo_coincide
from saldo_calculado sc
join public.inventario i on i.id_producto = sc.id_producto
order by sc.codigo;
