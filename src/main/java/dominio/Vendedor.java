package dominio;

import dominio.repositorio.RepositorioProducto;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Predicate;

import dominio.excepcion.GarantiaExtendidaException;
import dominio.repositorio.RepositorioGarantiaExtendida;

public class Vendedor {

	public static final String EL_PRODUCTO_TIENE_GARANTIA = "El producto ya cuenta con una garantia extendida";
	public static final String EL_PRODUCTO_NO_CUENTA_CON_GARANTIA = "Este producto no cuenta con garantia extendida";
	public static final String PARAMETROS_REQUERIDOS = "El codigo del producto y el nombre del cliente son requeridos";
	public static final String STRING_VACIO = "";
	public static final String[] VOCALES = { "a", "e", "i", "o", "u", "A", "E", "I", "O", "U" };

	private RepositorioProducto repositorioProducto;
	private RepositorioGarantiaExtendida repositorioGarantia;

	public Vendedor(RepositorioProducto repositorioProducto, RepositorioGarantiaExtendida repositorioGarantia) {
		this.repositorioProducto = repositorioProducto;
		this.repositorioGarantia = repositorioGarantia;

	}

	/**
	 * Método que se encarga de registrar una garantia extendida para un producto,
	 * tras realizar las validaciones de negocio requeridas
	 * 
	 * @param codigo        El código del producto sobre el que se desea generar una
	 *                      garantia extendida
	 * @param nombreCliente El nombre del cliente que solicita la garantia extendida
	 * 
	 * @throws GarantiaExtendidaException Excepción que se genera si no se cumplen
	 *                                    las validaciones de negocio u
	 *                                    obligatoriedad de campos
	 */
	public void generarGarantia(String codigo, String nombreCliente) throws GarantiaExtendidaException {
		if (codigo == null || codigo.isEmpty() || nombreCliente == null || nombreCliente.isEmpty())
			throw new GarantiaExtendidaException(PARAMETROS_REQUERIDOS);

		Predicate<String> predicate = Arrays.asList(VOCALES)::contains;
		if (Arrays.asList(codigo.split(STRING_VACIO)).stream().filter(predicate).count() == 3)
			throw new GarantiaExtendidaException(EL_PRODUCTO_NO_CUENTA_CON_GARANTIA);

		if (tieneGarantia(codigo))
			throw new GarantiaExtendidaException(EL_PRODUCTO_TIENE_GARANTIA);

		Producto producto = repositorioProducto.obtenerPorCodigo(codigo);
		double precioGarantia = calcularPrecioGarantia(producto.getPrecio());
		Date fechaSolicitudGarantia = new Date();
		Date fechaFinGarantia = calcularFechaFinGarantia(fechaSolicitudGarantia, precioGarantia);

		GarantiaExtendida garantiaExtendida = new GarantiaExtendida(producto, fechaSolicitudGarantia, fechaFinGarantia,
				precioGarantia, nombreCliente);
		repositorioGarantia.agregar(garantiaExtendida);
	}

	/**
	 * Método que se encarga de determinar si el producto asociado al código que
	 * ingresa como parámetro, cuenta con una garantia extendida
	 * 
	 * @param codigo El código del producto a validar
	 * 
	 * @return Boolean indicando si el producto cuenta o no con una garantia
	 *         extendida
	 */
	public boolean tieneGarantia(String codigo) {
		return repositorioGarantia.obtenerProductoConGarantiaPorCodigo(codigo) != null;
	}

	/**
	 * Método que se encarga de calcular el precio de la garantia extendida para un
	 * producto
	 * 
	 * @param precioProducto El precio del producto
	 * @return El precio de la garantia extendida
	 */
	public double calcularPrecioGarantia(double precioProducto) {
		return precioProducto > 500000 ? precioProducto * 0.2 : precioProducto * 0.1;
	}

	/**
	 * Método que se encarga de calcular la fecha final para la garantia extendida a
	 * partir del precio del producto y la fecha de solicitud
	 * 
	 * @param fechaSolicitudGarantia La fecha en la que se realiza la solicitud de
	 *                               la garantia extendida
	 * @param precioGarantia         El precio del producto sobre el que se realiza
	 *                               la solicitud de la garantia extendida
	 * 
	 * @return Fecha final de la garantia extendida
	 */
	public Date calcularFechaFinGarantia(Date fechaSolicitudGarantia, double precioGarantia) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(fechaSolicitudGarantia);

		if (precioGarantia > 500000) {
			int diaActualGarantia = 1;
			while (diaActualGarantia <= 200) {
				if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
					diaActualGarantia++;
				}
				calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
			if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
				calendar.add(Calendar.DAY_OF_YEAR, 2);
		} else {
			calendar.add(Calendar.DAY_OF_YEAR, 100);
		}
		return calendar.getTime();
	}

}
