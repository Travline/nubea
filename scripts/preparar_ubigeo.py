#!/usr/bin/env python3
"""Valida un CSV de UBIGEO normalizado y genera SQL transaccional por stdout."""
import csv
import re
import sys


def literal(valor):
    return "'" + valor.replace("'", "''") + "'"


def main():
    if len(sys.argv) != 2:
        raise ValueError('Uso: python3 scripts/preparar_ubigeo.py ubigeo.csv')
    filas = {}
    with open(sys.argv[1], encoding='utf-8-sig', newline='') as archivo:
        lector = csv.DictReader(archivo)
        esperados = ['ubigeo', 'departamento', 'provincia', 'distrito']
        if lector.fieldnames != esperados:
            raise ValueError('La cabecera debe ser ubigeo,departamento,provincia,distrito')
        for numero, fila in enumerate(lector, start=2):
            if None in fila or any(valor is None for valor in fila.values()):
                raise ValueError(f'Fila {numero}: columnas inválidas')
            codigo, departamento, provincia, distrito = (fila[campo].strip() for campo in esperados)
            if not re.fullmatch(r'[0-9]{6}', codigo) or not all((departamento, provincia, distrito)):
                raise ValueError(f'Fila {numero}: código o nombres inválidos')
            if any(len(x) > 100 or '\x00' in x for x in (departamento, provincia, distrito)):
                raise ValueError(f'Fila {numero}: nombre inválido')
            if codigo in filas:
                raise ValueError(f'Fila {numero}: código duplicado')
            filas[codigo] = (departamento, provincia, distrito)
    if not filas:
        raise ValueError('El archivo no contiene UBIGEO')
    print('BEGIN;')
    for codigo, nombres in sorted(filas.items()):
        valores = ','.join(literal(valor) for valor in (codigo, *nombres))
        print('INSERT INTO ubigeo(codigo_ubigeo,departamento,provincia,distrito) VALUES (' + valores + ') '
              'ON CONFLICT(codigo_ubigeo) DO UPDATE SET departamento=excluded.departamento, '
              'provincia=excluded.provincia,distrito=excluded.distrito;')
    print('COMMIT;')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, csv.Error) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
