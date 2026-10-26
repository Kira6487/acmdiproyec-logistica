-- Datos controlados para validar el inventario inicial en el entorno develop.
-- El saldo se genera mediante los triggers de movimiento; no se actualiza de forma aislada.
begin;

alter table public.productos
    add column stock_minimo numeric(14,3) not null default 0
        constraint ck_productos_stock_minimo_no_negativo check (stock_minimo >= 0);

insert into public.unidades_medida (codigo, nombre, abreviatura, activo)
values ('UNIDAD', 'Unidad', 'und', true)
on conflict (codigo) do update
set nombre = excluded.nombre,
    abreviatura = excluded.abreviatura,
    activo = true;

insert into public.categorias (codigo, nombre, descripcion, activo)
values
    ('DADOS_CONCRETO', 'Dados separadores de concreto',
        'Elementos prefabricados para controlar el recubrimiento del acero', true),
    ('ESCANTILLONES_CONCRETO', 'Escantillones de concreto',
        'Elementos prefabricados para mantener separaciones y dimensiones', true)
on conflict (codigo) do update
set nombre = excluded.nombre,
    descripcion = excluded.descripcion,
    activo = true;

insert into public.motivos_movimiento (codigo, nombre, tipo, descripcion, activo)
values (
    'ENTRADA_CARGA_INICIAL',
    'Carga inicial de inventario',
    'ENTRADA',
    'Ingreso controlado de existencias para pruebas en develop',
    true
)
on conflict (codigo) do update
set nombre = excluded.nombre,
    tipo = excluded.tipo,
    descripcion = excluded.descripcion,
    activo = true;

do $$
begin
    if not exists (select 1 from public.roles where codigo = 'LOGISTICA') then
        raise exception 'No existe el rol LOGISTICA requerido para la trazabilidad de la carga inicial';
    end if;
end;
$$;

insert into public.usuarios (
    id_rol,
    codigo,
    nombres,
    apellidos,
    correo,
    password_hash,
    activo
)
select
    r.id_rol,
    'SISTEMA_CARGA_INICIAL',
    'Sistema',
    'Carga inicial',
    'sistema-carga-inicial@invalid.local',
    '$disabled$' || replace(gen_random_uuid()::text, '-', ''),
    false
from public.roles r
where r.codigo = 'LOGISTICA'
on conflict (codigo) do update
set id_rol = excluded.id_rol,
    activo = false;

insert into public.productos (
    id_categoria,
    id_unidad_medida,
    codigo,
    nombre,
    descripcion,
    stock_minimo,
    activo
)
select
    c.id_categoria,
    u.id_unidad_medida,
    datos.codigo,
    datos.nombre,
    'Producto de prueba para validar la carga inicial de inventario',
    20.000,
    true
from (
    values
        ('DADO-2CM', 'Dado separador de concreto de 2 cm', 'DADOS_CONCRETO'),
        ('DADO-2.5CM', 'Dado separador de concreto de 2.5 cm', 'DADOS_CONCRETO'),
        ('DADO-3CM', 'Dado separador de concreto de 3 cm', 'DADOS_CONCRETO'),
        ('DADO-4CM', 'Dado separador de concreto de 4 cm', 'DADOS_CONCRETO'),
        ('DADO-5CM', 'Dado separador de concreto de 5 cm', 'DADOS_CONCRETO'),
        ('DADO-7CM', 'Dado separador de concreto de 7 cm', 'DADOS_CONCRETO'),
        ('DADO-7.5CM', 'Dado separador de concreto de 7.5 cm', 'DADOS_CONCRETO'),
        ('ESC-15CM', 'Escantillón de concreto de 15 cm', 'ESCANTILLONES_CONCRETO'),
        ('ESC-20CM', 'Escantillón de concreto de 20 cm', 'ESCANTILLONES_CONCRETO'),
        ('ESC-25CM', 'Escantillón de concreto de 25 cm', 'ESCANTILLONES_CONCRETO'),
        ('ESC-30CM', 'Escantillón de concreto de 30 cm', 'ESCANTILLONES_CONCRETO')
) as datos(codigo, nombre, codigo_categoria)
join public.categorias c on c.codigo = datos.codigo_categoria
join public.unidades_medida u on u.codigo = 'UNIDAD'
on conflict (codigo) do update
set id_categoria = excluded.id_categoria,
    id_unidad_medida = excluded.id_unidad_medida,
    nombre = excluded.nombre,
    descripcion = excluded.descripcion,
    stock_minimo = excluded.stock_minimo,
    activo = true;

insert into public.inventario (id_producto, stock, activo)
select p.id_producto, 0.000, true
from public.productos p
where p.codigo in (
    'DADO-2CM', 'DADO-2.5CM', 'DADO-3CM', 'DADO-4CM', 'DADO-5CM',
    'DADO-7CM', 'DADO-7.5CM', 'ESC-15CM', 'ESC-20CM', 'ESC-25CM', 'ESC-30CM'
)
on conflict (id_producto) do nothing;

insert into public.movimientos (
    codigo,
    id_motivo_movimiento,
    id_usuario,
    fecha_movimiento,
    tipo,
    estado,
    observacion,
    activo
)
select
    'CARGA-INICIAL-2026',
    mm.id_motivo_movimiento,
    u.id_usuario,
    '2026-08-24 00:00:00-05'::timestamptz,
    'ENTRADA',
    'VIGENTE',
    'Carga inicial de 100 unidades por producto para pruebas en develop',
    true
from public.motivos_movimiento mm
join public.usuarios u on u.codigo = 'SISTEMA_CARGA_INICIAL' and not u.activo
where mm.codigo = 'ENTRADA_CARGA_INICIAL'
  and mm.tipo = 'ENTRADA'
on conflict (codigo) do nothing;

insert into public.movimiento_detalle (id_movimiento, id_producto, cantidad, activo)
select m.id_movimiento, p.id_producto, 100.000, true
from public.movimientos m
join public.productos p on p.codigo in (
    'DADO-2CM', 'DADO-2.5CM', 'DADO-3CM', 'DADO-4CM', 'DADO-5CM',
    'DADO-7CM', 'DADO-7.5CM', 'ESC-15CM', 'ESC-20CM', 'ESC-25CM', 'ESC-30CM'
)
where m.codigo = 'CARGA-INICIAL-2026'
on conflict (id_movimiento, id_producto) do nothing;

do $$
declare
    productos_validos integer;
    detalles_validos integer;
    saldos_inconsistentes integer;
begin
    select count(*)
      into productos_validos
      from public.productos p
      join public.inventario i on i.id_producto = p.id_producto
     where p.codigo in (
         'DADO-2CM', 'DADO-2.5CM', 'DADO-3CM', 'DADO-4CM', 'DADO-5CM',
         'DADO-7CM', 'DADO-7.5CM', 'ESC-15CM', 'ESC-20CM', 'ESC-25CM', 'ESC-30CM'
     )
       and p.stock_minimo = 20.000
       and i.stock = 100.000
       and p.activo
       and i.activo;

    if productos_validos <> 11 then
        raise exception 'La carga inicial no dejó 11 productos activos con stock mínimo 20 y stock 100';
    end if;

    select count(*)
      into detalles_validos
      from public.movimiento_detalle md
      join public.movimientos m on m.id_movimiento = md.id_movimiento
     where m.codigo = 'CARGA-INICIAL-2026'
       and m.tipo = 'ENTRADA'
       and m.estado = 'VIGENTE'
       and md.cantidad = 100.000
       and md.activo;

    if detalles_validos <> 11 then
        raise exception 'La carga inicial no contiene los 11 detalles de entrada esperados';
    end if;

    with saldo_calculado as (
        select
            p.id_producto,
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
        group by p.id_producto
    )
    select count(*)
      into saldos_inconsistentes
      from saldo_calculado sc
      join public.inventario i on i.id_producto = sc.id_producto
     where i.stock <> sc.stock_segun_movimientos;

    if saldos_inconsistentes <> 0 then
        raise exception 'Existen saldos que no coinciden con el historial de movimientos';
    end if;
end;
$$;

commit;
