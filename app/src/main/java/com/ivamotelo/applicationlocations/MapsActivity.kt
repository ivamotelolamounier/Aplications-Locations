/**
 * Exemplo de como utilizar a API do GoogleMap
 * Documentação:
 * https://developer.android.com/reference/android/location/Location
 * Também foi habilitado o 'AutoImports' nas configurações gerais, para que
 * o próprio AndroidStudio faça as importações necessárias, automáticamente
 *
 */
package com.ivamotelo.applicationlocations

import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.ivamotelo.applicationlocations.databinding.ActivityMapsBinding
import java.util.*

class MapsActivity : AppCompatActivity(),
    OnMapReadyCallback, // segunda chamada - onde ficará a função setUpMap() do usuário
    GoogleMap.OnMarkerClickListener{

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var lastLocation : Location

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    // Constante para manipular o mapa através do usuário (permissão)
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
            }
        }
        createLocationRequest()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        /**
         * Este código já carrega o mapa com os dados predeterminados
         * map = googleMap
         * // Add a marker in favorite city and move the camera
         * val myPlace = LatLng(-20.771674, -45.273124)
         * map.addMarker(MarkerOptions().position(myPlace).title("Minha cidade favorita"))
         * // move o mapa para o local selecionado em 'myPlace'
         * // map.moveCamera(CameraUpdateFactory.newLatLng(myPlace))
         * // a mesma propriedade câmera, porém, com um zoon já definido
         * map.moveCamera(CameraUpdateFactory.newLatLngZoom(myPlace, 12.0f))
         */

        /**
         * Este código permite que o usuário escolha qual localização deseja
         */
        map = googleMap
        // implementa botões de + - zoon
        map.uiSettings.isZoomControlsEnabled = true
        // habilita dois comportamentos do maps: traçar rotas ou marcadores
        map.setOnMarkerClickListener(this)
        setUpMap()
    }

    /**
     * Função para conceder permissão de localização e deixar o usuário escolher
     * o local que deseja acessar no mapa
     * 1 - Requisita as permissões de localização
     * 2 -
     */
    private fun setUpMap() {
        // Verifica se a Activity possui a permissão de localização
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // Requer a permissão no array de permissões do Manifest.xml
                        ActivityCompat.requestPermissions(this,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
                return
        }
        // Habilita a localização do usuário (icone do alvo no mapa)
        map.isMyLocationEnabled = true
        // configura os tipos de mapas: Relevo, normal, satélite, terreno
        map.mapType = GoogleMap.MAP_TYPE_HYBRID

        /**
        Se encontrar a localização, será adicionado um 'sucessListener'.
        Pode ocorrer em raras situações que a localização seja nula, para isso
        tratar a excessão com um 'if'
        */
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastLocation = location
                // Atribui a localização atual á variável 'currentLatLng'
                val currentLatLng = LatLng(location.latitude, location.longitude)
                // passa a localização atual para a função 'placeMarkerOnMap()
                placeMarkerOnMap(currentLatLng)
                // carrega 'animate' a localização atual e exibe na tela do app
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12.0f))
            }
        }
    }

    /**
     * Esta função possibilita a criação de 'marker' (marcas de localização) no mapa
     * è passada a latitude e a longitude (LatLng) e após, a mesma é fixada no mapa
     * pelo método 'addMarker()'.
     * No exemplo foram utilizados novos icons para substituir os icones padrão do maps
     * Também pode-se implementar a função 'getAdress()' conforme a mesma abaixo
     *
     */
    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions().position(location)
        // implementa a função 'getadress()'
        val titleStr = getAddress(location)
        markerOptions.title(titleStr)
        // Substitui os icones padrão do maps
        // markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources, R.mipmap.ic_user_location)))
        map.addMarker(markerOptions)
    }

    /**
     * Função que possibilita mostrar o endereço selecionado pelo usuário
     * através da latitude e longetude informada
     */
    private fun getAddress(latLng: LatLng): String? {
        val geocoder : Geocoder
        val addresses : List<Address>

        geocoder = Geocoder(this, Locale.getDefault())
        addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) // um só resultado mais relevante de muitos retornados

        val address = addresses[0].getAddressLine(0)
        val city = addresses[0].locality
        val state = addresses[0].adminArea
        val country = addresses[0].countryCode
        val postalCode = addresses[0].postalCode
        return address
    }

    /**
     * Função auxiliar utilizada para verificar a nova posição, a medida que usuário for se movendo pelo terreno.
     * 1 - Solicita a permissão de localização ao usuário
     * 2 - Se a permissão for concedida, 'fusedLocationClient' realiza um loop permanente
     * verificando contínuamente a localização autal do usuário
     */
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        // loop para fazer as atualizações do usuário
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    /**
     * função que marca no mapa as novas posições do usuário quando o mesmo se move pelo mapa
     *
     */
    private fun createLocationRequest(){
        locationRequest = LocationRequest()
        locationRequest.interval = 10000   //intevalo para verificar a movimentação usuário
        locationRequest.fastestInterval = 5000 //intevalo rápido para as atualizações
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //Alta prioridade

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // se a selecção (click no mapa) tiver sucesso, é chamada a função 'startLocationState()'
        task.addOnSuccessListener{
            locationUpdateState = true
            startLocationUpdates()
        }
        // Se a localização selecionada não for satisfeita, mas puder ser fixada, então inicia-se
        // um diálago de permissão com o usuário
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException){
                try {
                    //mostra o diálago chamando 'startResolutionForResult()' e checa o resultado
                    //na 'onActivityResult()'
                    e.startResolutionForResult(this@MapsActivity,
                    REQUEST_CHECK_SETTINGS)
                } catch (senEx : IntentSender.SendIntentException) {
                    // ignora o erro
                }
            }
        }
    }

    /**
     * Quanto o aplicativo estiver no ciclo de vida 'OnPause()', ou seja minimizado,
     * dentro da função, será removido as localizações do usuário para que seja
     * preservada a bateria do aparelho em razão do alto consumo do GPS, uma vez que
     * estando a aplicação fechada não há necessidade de atualizações de localização
     */
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Pelo Ciclo de vida da aplicação, primeiro é chamado o 'onCreate()' e após
     * o 'onResume()'
     * Do mesmo modo, voltando a atividade do ciclo de vida da aplicação, será então
     * restabelecida a função 'startLocationUpdates()' para reiniciar as atualizações
     * de movimentação do usuário.
     * è realizada nova checagem (if) para verificar se não estiver habilitado a função
     * 'locationUpdateState()', requer que a mesma seja reativada
     */
    override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }
    override fun onMarkerClick(p0: Marker) = false
}