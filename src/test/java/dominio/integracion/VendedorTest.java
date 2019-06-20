package dominio.integracion;

import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dominio.Vendedor;
import dominio.GarantiaExtendida;
import dominio.Producto;
import dominio.excepcion.GarantiaExtendidaException;
import dominio.repositorio.RepositorioProducto;
import dominio.repositorio.RepositorioGarantiaExtendida;
import persistencia.sistema.SistemaDePersistencia;
import testdatabuilder.ProductoTestDataBuilder;

public class VendedorTest {

	private static final String COMPUTADOR_LENOVO = "Computador Lenovo";
	private static final String CLIENTE_PRUEBA = "ClientePrueba";
	private static final String CODIGO_PRODUCTO_PRUEBA = "FE1TSA0A50";

	private static final String FECHA_SOLICITUD_GARANTIA = "16/08/2018";
	private static final String FECHA_FIN_GARANTIA = "06/04/2019";

	private static final String FECHA_SOLICITUD_GARANTIA_FINALIZACION_DOMINGO = "17/08/2018";
	private static final String FECHA_FIN_GARANTIA_FINALIZACION_DOMINGO = "09/04/2019";

	private static final String FECHA_SOLICITUD_GARANTIA_BAJO_UMBRAL = "16/08/2018";
	private static final String FECHA_FIN_GARANTIA_BAJO_UMBRAL = "24/11/2018";

	private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

	private SistemaDePersistencia sistemaPersistencia;

	private RepositorioProducto repositorioProducto;
	private RepositorioGarantiaExtendida repositorioGarantia;

	@Before
	public void setUp() {

		sistemaPersistencia = new SistemaDePersistencia();

		repositorioProducto = sistemaPersistencia.obtenerRepositorioProductos();
		repositorioGarantia = sistemaPersistencia.obtenerRepositorioGarantia();

		sistemaPersistencia.iniciar();
	}

	@After
	public void tearDown() {
		sistemaPersistencia.terminar();
	}

	/**
	 * Método que permite verificar que la garantia extendida se esta persistiendo
	 * de forma correcta para un producto dado. Validación regla de negocio 4.
	 * 
	 * RESULTADO ESPERADO: El producto tiene una garantia, el cliente y el código
	 * del producto concuerdan con los datos de la garantia registrada
	 */
	@Test
	public void generarGarantiaTest() {

		// arrange
		Producto producto = new ProductoTestDataBuilder().conNombre(COMPUTADOR_LENOVO).build();
		repositorioProducto.agregar(producto);
		Vendedor vendedor = new Vendedor(repositorioProducto, repositorioGarantia);

		// act
		vendedor.generarGarantia(producto.getCodigo(), CLIENTE_PRUEBA);

		// assert
		Assert.assertTrue(vendedor.tieneGarantia(producto.getCodigo()));
		Assert.assertNotNull(repositorioGarantia.obtenerProductoConGarantiaPorCodigo(producto.getCodigo()));

		GarantiaExtendida garantia = repositorioGarantia.obtener(producto.getCodigo());

		Assert.assertEquals(CLIENTE_PRUEBA, garantia.getNombreCliente());
		Assert.assertEquals(producto.getCodigo(), garantia.getProducto().getCodigo());
	}

	/**
	 * Método que permite verificar que los parámetros requeridos para la generación
	 * de una garantia sean enviados. Validación regla de negocio 1.
	 * 
	 * RESULTADO ESPERADO: Excepción que indica los parametros codigoProducto y
	 * nombreCliente como requeridos
	 */
	@Test
	public void parametrosRequeridosGarantia() {
		Vendedor vendedor = new Vendedor(repositorioProducto, repositorioGarantia);
		try {
			vendedor.generarGarantia(null, "");
			fail();
		} catch (GarantiaExtendidaException e) {
			// assert
			Assert.assertEquals(Vendedor.PARAMETROS_REQUERIDOS, e.getMessage());
		}
	}

	/**
	 * Método que permite verificar que no se puede asociar más de una garantia a un
	 * producto. Validación regla de negocio 2.
	 * 
	 * RESULTADO ESPERADO: Excepción que indica que el producto ya cuenta con una
	 * garantia.
	 */
	@Test
	public void productoYaTieneGarantiaTest() {

		// arrange
		Producto producto = new ProductoTestDataBuilder().conNombre(COMPUTADOR_LENOVO).build();

		repositorioProducto.agregar(producto);

		Vendedor vendedor = new Vendedor(repositorioProducto, repositorioGarantia);

		// act
		vendedor.generarGarantia(producto.getCodigo(), CLIENTE_PRUEBA);
		try {

			vendedor.generarGarantia(producto.getCodigo(), CLIENTE_PRUEBA);
			fail();

		} catch (GarantiaExtendidaException e) {
			// assert
			Assert.assertEquals(Vendedor.EL_PRODUCTO_TIENE_GARANTIA, e.getMessage());
		}
	}

	/**
	 * Método que permite verificar que un producto con un codigo que cuenta con 3
	 * vocales no cuenta con garantia extendida. Validación regla de negocio 3.
	 * 
	 * RESULTADO ESPERADO: Excepción que indica que el producto no cuenta con
	 * garantia
	 */
	@Test
	public void productoNoCuentaConGarantia() {
		// arrange
		Producto producto = new ProductoTestDataBuilder().conNombre(COMPUTADOR_LENOVO).conCodigo(CODIGO_PRODUCTO_PRUEBA)
				.build();

		repositorioProducto.agregar(producto);

		Vendedor vendedor = new Vendedor(repositorioProducto, repositorioGarantia);

		// act
		try {
			vendedor.generarGarantia(producto.getCodigo(), CLIENTE_PRUEBA);
			fail();
		} catch (GarantiaExtendidaException e) {
			Assert.assertEquals(Vendedor.EL_PRODUCTO_NO_CUENTA_CON_GARANTIA, e.getMessage());
		}
	}

	/**
	 * Método que permite vertificar que una garantia extendida es persistida para
	 * un producto con el precio sobre el umbral 500000. Validacion regla de negocio
	 * 5 - Flujo 1 (Precio sobre el umbral, fecha de finalización de la garantia en
	 * un día hábil)
	 * 
	 * RESULTADO ESPERADO: El código del producto, el precio de la garantia y la
	 * fecha final de la garantia concuerdan con los datos esperados
	 * 
	 * @throws ParseException Excepción generada al transformar fechas
	 */
	@Test
	public void generarGarantiaProductoSobreUmbral() throws ParseException {
		Producto producto = new ProductoTestDataBuilder().conNombre(COMPUTADOR_LENOVO).build();
		repositorioProducto.agregar(producto);
		Vendedor vendedor = new Vendedor(repositorioProducto, repositorioGarantia);

		Date fechaSolicitudGarantia = sdf.parse(FECHA_SOLICITUD_GARANTIA);
		Calendar fecha = Calendar.getInstance();
		fecha.setTime(fechaSolicitudGarantia);

		double precioGarantia = vendedor.calcularPrecioGarantia(producto.getPrecio());
		Date fechaFinGarantia = vendedor.calcularFechaFinGarantia(fechaSolicitudGarantia, producto.getPrecio());

		GarantiaExtendida garantiaExtendida = new GarantiaExtendida(producto, fechaSolicitudGarantia, fechaFinGarantia,
				precioGarantia, CLIENTE_PRUEBA);

		repositorioGarantia.agregar(garantiaExtendida);

		GarantiaExtendida garantia = repositorioGarantia.obtener(producto.getCodigo());

		Assert.assertNotNull(garantia);
		Assert.assertEquals(producto.getCodigo(), garantia.getProducto().getCodigo());
		Assert.assertEquals(Double.valueOf(producto.getPrecio() * 0.2).intValue(),
				Double.valueOf(garantia.getPrecioGarantia()).intValue());
		Assert.assertEquals(FECHA_FIN_GARANTIA, sdf.format(garantia.getFechaFinGarantia()));
	}

	/**
	 * Método que permite verificar que una garantia extendida es persistida para un
	 * producto con el precio sobre el umbral 500000. Validacion regla de negocio 5
	 * - Flujo 2 (Precio sobre el umbral, fecha de finalización de la garantia en un
	 * día domingo)
	 * 
	 * RESULTADO ESPERADO: El código del producto, el precio de la garantia y la
	 * fecha final de la garantia concuerdan con los datos esperados
	 * 
	 * @throws ParseException Excepción generada al transformar las fechas
	 */
	@Test
	public void generarGarantiaProductoSobreUmbralFinalizacionDomingo() throws ParseException {
		Producto producto = new ProductoTestDataBuilder().conNombre(COMPUTADOR_LENOVO).build();
		repositorioProducto.agregar(producto);
		Vendedor vendedor = new Vendedor(repositorioProducto, repositorioGarantia);

		Date fechaSolicitudGarantia = sdf.parse(FECHA_SOLICITUD_GARANTIA_FINALIZACION_DOMINGO);
		Calendar fecha = Calendar.getInstance();
		fecha.setTime(fechaSolicitudGarantia);

		double precioGarantia = vendedor.calcularPrecioGarantia(producto.getPrecio());
		Date fechaFinGarantia = vendedor.calcularFechaFinGarantia(fechaSolicitudGarantia, producto.getPrecio());

		GarantiaExtendida garantiaExtendida = new GarantiaExtendida(producto, fechaSolicitudGarantia, fechaFinGarantia,
				precioGarantia, CLIENTE_PRUEBA);

		repositorioGarantia.agregar(garantiaExtendida);

		GarantiaExtendida garantia = repositorioGarantia.obtener(producto.getCodigo());

		Assert.assertNotNull(garantia);
		Assert.assertEquals(producto.getCodigo(), garantia.getProducto().getCodigo());
		Assert.assertEquals(Double.valueOf(producto.getPrecio() * 0.2).intValue(),
				Double.valueOf(garantia.getPrecioGarantia()).intValue());
		Assert.assertEquals(FECHA_FIN_GARANTIA_FINALIZACION_DOMINGO, sdf.format(garantia.getFechaFinGarantia()));
	}

	/**
	 * Método que permite verificar que una garantia extendida es persistida para un
	 * producto con el precio bajo el umbral 500000. Validacion regla de negocio 5 -
	 * Flujo 3 (Precio bajo el umbral)
	 * 
	 * RESULTADO ESPERADO: El código del producto, el precio de la garantia y la
	 * fecha final de la garantia concuerdan con los datos esperados
	 * 
	 * @throws ParseException Excepción generada al transformar las fechas
	 */
	@Test
	public void generarGarantiaProductoBajoUmbral() throws ParseException {
		Producto producto = new ProductoTestDataBuilder().conNombre(COMPUTADOR_LENOVO).conPrecio(200000).build();
		repositorioProducto.agregar(producto);
		Vendedor vendedor = new Vendedor(repositorioProducto, repositorioGarantia);

		Date fechaSolicitudGarantia = sdf.parse(FECHA_SOLICITUD_GARANTIA_BAJO_UMBRAL);
		Calendar fecha = Calendar.getInstance();
		fecha.setTime(fechaSolicitudGarantia);

		double precioGarantia = vendedor.calcularPrecioGarantia(producto.getPrecio());
		Date fechaFinGarantia = vendedor.calcularFechaFinGarantia(fechaSolicitudGarantia, producto.getPrecio());

		GarantiaExtendida garantiaExtendida = new GarantiaExtendida(producto, fechaSolicitudGarantia, fechaFinGarantia,
				precioGarantia, CLIENTE_PRUEBA);

		repositorioGarantia.agregar(garantiaExtendida);

		GarantiaExtendida garantia = repositorioGarantia.obtener(producto.getCodigo());

		Assert.assertNotNull(garantia);
		Assert.assertEquals(producto.getCodigo(), garantia.getProducto().getCodigo());
		Assert.assertEquals(Double.valueOf(producto.getPrecio() * 0.1).intValue(),
				Double.valueOf(garantia.getPrecioGarantia()).intValue());
		Assert.assertEquals(FECHA_FIN_GARANTIA_BAJO_UMBRAL, sdf.format(garantia.getFechaFinGarantia()));
	}

}
