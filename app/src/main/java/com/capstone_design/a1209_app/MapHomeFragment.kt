package com.capstone_design.a1209_app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import com.capstone_design.a1209_app.board.BoardWirteActivity
import com.capstone_design.a1209_app.dataModels.addressData
import com.capstone_design.a1209_app.dataModels.dataModel
import com.capstone_design.a1209_app.databinding.FragmentMapHomeBinding
import com.capstone_design.a1209_app.fragment.HomeFragment
import com.capstone_design.a1209_app.utils.FBRef
import com.capstone_design.map_test.FragmentListener
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import java.util.jar.Manifest


class MapHomeFragment : Fragment(), FragmentListener, OnMapReadyCallback {
    private  lateinit var binding : FragmentMapHomeBinding
    private lateinit var mFragmentListener: FragmentListener
    val viewPagerList= mutableListOf<dataModel>()
    private lateinit var auth: FirebaseAuth
    private lateinit var mView: MapView
    private lateinit var mMap:GoogleMap
    lateinit var mainActivity: MainActivity
    private lateinit var myLatLng:LatLng
    private lateinit var cardView:CardView
    private lateinit var viewPager2: ViewPager2
    private lateinit var springDotsIndicator: SpringDotsIndicator

    private var category="all"
    private var cnt=0
    private var items= mutableListOf<dataModel>()
    private val itemsKeyList= mutableListOf<String>()

    val database = Firebase.database

    //viewpager
    private var bannerPosition = Int.MAX_VALUE/2
    private val intervalTime = 1500.toLong()


    val permission=arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION)
    val PERM_FLAG=99
    override fun onAttach(context: Context) {
        super.onAttach(context)

        mainActivity =context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding= DataBindingUtil.inflate(inflater,R.layout.fragment_map_home, container, false)

        auth = Firebase.auth
        val database = Firebase.database
        val schRef:DatabaseReference =
            database.getReference("users").child(auth.currentUser?.uid.toString()).child("address")
        schRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (DataModel in snapshot.children) {
                    val item = DataModel.getValue(addressData::class.java)
                    if (item != null) {
                        if(item.set=="1")
                            binding.addressTv.text=item.address
                        Log.d("mhf","호출")
                        //좌표 가져와서 지도에 초점 맞추기
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
        if(cnt==0) {
            listViewAll()
            buttonColor("all")
            cnt+=1
        }
        viewPager2=binding.viewPager
        springDotsIndicator=binding.springDotsIndicator





//        //버튼 클릭시 category에 값 할당하기
//        binding.categoryAll.setOnClickListener {
//            category="all"
//            listViewAll()
//            buttonColor("all")
//        }
//        binding.categoryAsian.setOnClickListener {
//            category="asian"
//            listView("asian")
//            buttonColor("asian")
//        }
//        binding.categoryBun.setOnClickListener {
//            listView("bun")
//            buttonColor("bun")
//        }
//        binding.categoryChicken.setOnClickListener {
//            category = "chicken"
//            listView("chicken")
//            buttonColor("chicken")
//        }
//        binding.categoryPizza.setOnClickListener {
//            category = "pizza"
//            listView("chicken")
//            buttonColor("pizza")
//        }
//        binding.categoryFast.setOnClickListener {
//            category = "fastfood"
//            listView("fastfood")
//            buttonColor("fast")
//        }
//        binding.categoryJap.setOnClickListener {
//            category = "japan"
//            listView("japan")
//            buttonColor("japan")
//        }
//        binding.categoryKor.setOnClickListener {
//            category = "korean"
//            listView("korean")
//            buttonColor("korean")
//        }
//        binding.categoryDo.setOnClickListener {
//            category = "bento"
//            listView("bento")
//            buttonColor("bento")
//        }
//        binding.categoryCafe.setOnClickListener {
//            category = "cafe"
//            listView("cafe")
//            buttonColor("cafe")
//        }
//        binding.categoryChi.setOnClickListener {
//            category = "chi"
//            listView("chi")
//            buttonColor("chi")
//        }

        binding.mapGo.setOnClickListener {
            val intent = Intent(context, AddressSearchActivity::class.java).putExtra("mhf","1")
                .putExtra("page","MapHomeFragment")
            startActivity(intent)
        }
        mView=binding.mapView
        binding.writeBtn.setOnClickListener {
            val intent = Intent(context, BoardWirteActivity::class.java)
            startActivity(intent)
        }
        mView.onCreate(savedInstanceState)

        //현재 내 위치 가져오는

        if(isPermitted()){
            //onMapReady함수 호출
            Log.d("mhf","startProcess")
            startProcess()
        }else{
            requestPermissions(permission,PERM_FLAG)
        }

        //글목록으로 이동
        binding.btn.setOnClickListener {
           //버튼 누르면
            mFragmentListener = parentFragment as HomeFragment
            mFragmentListener.onReceivedData(1)
        }
        return binding.root
    }

    override fun onReceivedData(data: Int) {
    }

    fun startProcess(){
        mView.getMapAsync(this)
    }

    fun isPermitted():Boolean{
        for(perm in permission){
            if(checkSelfPermission(mainActivity,perm)!=PERMISSION_GRANTED){
                return false
            }
        }
        return true
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap=googleMap

        binding.myloc.setOnClickListener {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(mainActivity)
            setUpdateLocationListener()
        }

        auth = Firebase.auth
        val database = Firebase.database
        val schRef :DatabaseReference= database.getReference("users").child(auth.currentUser?.uid.toString()).child("address")
        schRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (DataModel in snapshot.children) {
                    val item = DataModel.getValue(addressData::class.java)
                    if (item != null) {
                        if (item.set=="1") {
                            val myLocation=LatLng(item.lat.toDouble(),item.lng.toDouble())
                            val discripter = getMarkerDrawable(R.drawable.marker)
                            val marker = MarkerOptions()
                                .position(myLocation)
                                .title(item.name)
                                .icon(discripter)
                            //Log.d("item",item.toString())
                            val cameraOption = CameraPosition.Builder()
                                .target(myLocation)
                                .zoom(17f)
                                .build()
                            val camera = CameraUpdateFactory.newCameraPosition(cameraOption)

//                            mMap.addMarker(marker)
                            mMap.moveCamera(camera)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        markerView()

        //마커 클릭 시 카드 뷰 보이게 하기
        mMap!!.setOnMarkerClickListener (object :GoogleMap.OnMarkerClickListener{
            override fun onMarkerClick(p0: Marker): Boolean {
                markerClick(p0.tag.toString())
                viewPager2.visibility=View.VISIBLE
                springDotsIndicator.visibility=View.VISIBLE



                return false
            }


        })
        mMap!!.setOnMapClickListener(object : GoogleMap.OnMapClickListener {
            override fun onMapClick(latLng: LatLng) {
               viewPager2.visibility = View.GONE
                   springDotsIndicator.visibility = View.GONE

                Log.d("latlng",latLng.toString())

            }
        })




    }

    //내 위치를 가져오는 코드
    lateinit var fusedLocationClient:FusedLocationProviderClient
    lateinit var locationCallback:LocationCallback

    @SuppressLint("MissingPermission")
    fun setUpdateLocationListener(){
        val locationRequest=LocationRequest.create()
        locationRequest.run{
            priority=LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback=object  : LocationCallback (){
            override fun onLocationResult(p0: LocationResult) {
                p0?.let{
                   for ((i,location)  in it.locations.withIndex()){
                       Log.d("로케이션","$i ${location.latitude},${location.longitude}")
                        setLastLocation(location)

                   }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper())
        //로케이션 요청 함수 호출
    }
    fun setLastLocation(location: Location){
        val myLocation=LatLng(location.latitude,location.longitude)
        val discripter=getMarkerDrawable(R.drawable.marker)
        val marker=MarkerOptions()
            .position(myLocation)
            .title("I'm here")
            .icon(discripter)

        val cameraOption = CameraPosition.Builder()
            .target(myLocation)//현재 위치로 바꿀 것
            .zoom(17f)
            .build()
        val camera=CameraUpdateFactory.newCameraPosition(cameraOption)

        mMap.clear()
        mMap.addMarker(marker)
        mMap.moveCamera(camera)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERM_FLAG->{
                var check=true
                for(grant in grantResults){
                    if(grant!= PERMISSION_GRANTED){
                        check = false
                        break
                    }
                }
                if(check){
                    startProcess()
                }else{
                    Toast.makeText(mainActivity, "권한을 승인해야지만 앱 사용가능",Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        mView.onStart()
    }
    override fun onStop() {
        super.onStop()
        mView.onStop()
    }
    override fun onResume() {
        super.onResume()
        mView.onResume()
    }
    override fun onPause() {
        super.onPause()
        mView.onPause()
    }
    override fun onLowMemory() {
        super.onLowMemory()
        mView.onLowMemory()
    }
    override fun onDestroy() {
        mView.onDestroy()
        super.onDestroy()
    }

    fun getMarkerDrawable(drawableId:Int):BitmapDescriptor{
        //마커 아이콘 만들기
        var bitmapDrawable:BitmapDrawable
        bitmapDrawable=mainActivity.resources.getDrawable(drawableId)as BitmapDrawable

        //마커 크기 변환(크게)
        val scaleBitmap= Bitmap.createScaledBitmap(bitmapDrawable.bitmap,150,235,false)
        return BitmapDescriptorFactory.fromBitmap(scaleBitmap)
    }
    private fun buttonColor(category:String){
        binding.categoryAsian.setBackgroundResource(R.drawable.round_button)
        binding.categoryAsian.setTextColor(Color.BLACK)
        binding.categoryAsian.setTypeface(binding.categoryAsian.typeface, Typeface.NORMAL)


        binding.categoryBun.setBackgroundResource(R.drawable.round_button)
        binding.categoryBun.setTextColor(Color.BLACK)
        binding.categoryBun.setTypeface(binding.categoryAsian.typeface, Typeface.NORMAL)


        binding.categoryKor.setBackgroundResource(R.drawable.round_button)
        binding.categoryKor.setTextColor(Color.BLACK)
        binding.categoryKor.setTypeface(binding.categoryAsian.typeface, Typeface.NORMAL)


        binding.categoryJap.setBackgroundResource(R.drawable.round_button)
        binding.categoryJap.setTextColor(Color.BLACK)
        binding.categoryJap.setTypeface(binding.categoryAsian.typeface, Typeface.NORMAL)



        binding.categoryChi.setBackgroundResource(R.drawable.round_button)
        binding.categoryChi.setTextColor(Color.BLACK)
        binding.categoryChi.setTypeface(binding.categoryAsian.typeface, Typeface.NORMAL)


        binding.categoryFast.setBackgroundResource(R.drawable.round_button)
        binding.categoryFast.setTextColor(Color.BLACK)
        binding.categoryFast.setTypeface(binding.categoryAsian.typeface, Typeface.NORMAL)


        binding.categoryDo.setBackgroundResource(R.drawable.round_button)
        binding.categoryDo.setTextColor(Color.BLACK)
        binding.categoryDo.setTypeface(binding.categoryAsian.typeface, Typeface.NORMAL)



        binding.categoryCafe.setBackgroundResource(R.drawable.round_button)
        binding.categoryCafe.setTextColor(Color.BLACK)
        binding.categoryCafe.setTypeface(binding.categoryAsian.typeface, Typeface.NORMAL)


        binding.categoryChicken.setBackgroundResource(R.drawable.round_button)
        binding.categoryChicken.setTextColor(Color.BLACK)
        binding.categoryChicken.setTypeface(binding.categoryAsian.typeface, Typeface.NORMAL)


        binding.categoryPizza.setBackgroundResource(R.drawable.round_button)
        binding.categoryPizza.setTextColor(Color.BLACK)
        binding.categoryPizza.setTypeface(binding.categoryAsian.typeface, Typeface.NORMAL)

        binding.categoryAll.setBackgroundResource(R.drawable.round_button)
        binding.categoryAll.setTextColor(Color.BLACK)
        binding.categoryAll.setTypeface(binding.categoryAll.typeface, Typeface.NORMAL)


        when(category){
            "asian"->{binding.categoryAsian.setBackgroundResource(R.drawable.select_round)
                binding.categoryAsian.setTextColor(Color.WHITE)
                binding.categoryAsian.setTypeface(binding.categoryAsian.typeface, Typeface.BOLD)}
            "bun"->{binding.categoryBun.setBackgroundResource(R.drawable.select_round)
                binding.categoryBun.setTextColor(Color.WHITE)
                binding.categoryBun.setTypeface(binding.categoryBun.typeface, Typeface.BOLD)}
            "korean"->{binding.categoryKor.setBackgroundResource(R.drawable.select_round)
                binding.categoryKor.setTextColor(Color.WHITE)
                binding.categoryKor.setTypeface(binding.categoryKor.typeface, Typeface.BOLD)}
            "japan"->{binding.categoryJap.setBackgroundResource(R.drawable.select_round)
                binding.categoryJap.setTextColor(Color.WHITE)
                binding.categoryJap.setTypeface(binding.categoryJap.typeface, Typeface.BOLD)}
            "chi"->{binding.categoryChi.setBackgroundResource(R.drawable.select_round)
                binding.categoryChi.setTextColor(Color.WHITE)
                binding.categoryChi.setTypeface(binding.categoryChi.typeface, Typeface.BOLD)}
            "fast"->{binding.categoryFast.setBackgroundResource(R.drawable.select_round)
                binding.categoryFast.setTextColor(Color.WHITE)
                binding.categoryFast.setTypeface(binding.categoryFast.typeface, Typeface.BOLD)}
            "bento"->{binding.categoryDo.setBackgroundResource(R.drawable.select_round)
                binding.categoryDo.setTextColor(Color.WHITE)
                binding.categoryDo.setTypeface(binding.categoryDo.typeface, Typeface.BOLD)}
            "cafe"->{binding.categoryCafe.setBackgroundResource(R.drawable.select_round)
                binding.categoryCafe.setTextColor(Color.WHITE)
                binding.categoryCafe.setTypeface(binding.categoryCafe.typeface, Typeface.BOLD)}
            "chicken"->{binding.categoryChicken.setBackgroundResource(R.drawable.select_round)
                binding.categoryChicken.setTextColor(Color.WHITE)
                binding.categoryChicken.setTypeface(binding.categoryChicken.typeface, Typeface.BOLD)}
            "pizza"->{binding.categoryPizza.setBackgroundResource(R.drawable.select_round)
                binding.categoryPizza.setTextColor(Color.WHITE)
                binding.categoryPizza.setTypeface(binding.categoryPizza.typeface, Typeface.BOLD)}
            "all"->{binding.categoryAll.setBackgroundResource(R.drawable.select_round)
                binding.categoryAll.setTextColor(Color.WHITE)
                binding.categoryAll.setTypeface(binding.categoryAll.typeface, Typeface.BOLD)}

        }

    }
    //모든 data담아두는 List
    private var dataList= mutableListOf<dataModel>()
    private var tempList= mutableListOf<dataModel>()
    private fun markerView(){
        val boardRef : DatabaseReference = database.getReference("map_contents")
        boardRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dataList.clear()
                tempList.clear()
                for (data in snapshot.children) {
                    val item = data.getValue(dataModel::class.java)
                    if (item != null) {
                        Log.d("category", category)
                            val latLng = LatLng(item.lat.toDouble(), item.lng.toDouble())
                            val discripter = getMarkerDrawable(R.drawable.marker)
                            val markerOptions = MarkerOptions()
                                .position(latLng)
                                .icon(discripter)
                            dataList.add(item)
                            itemsKeyList.add(data.key.toString())
                            //주소가 같은 것이 있으면 제외하기-> continue
                            var con=false
                            for(i in tempList){
                                if(i.placeAddress==item.placeAddress){
                                    con=true
                                    break
                                }
                            }
                            if(con){
                                continue
                            }
                            val marker: Marker? = mMap!!.addMarker(markerOptions)
                            marker!!.tag = item.placeAddress //나중에 place=address+detail 분리하기
                            tempList.add(item)//새로운 주소 목록에 포함.
                        } } }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun markerClick(tag:String){
        //마커가 클릭 됐을 때 같은 주소끼리 배열로 만들어서 viewPager에 보이게 하기
        //같은 placeAddress를 배열로 묶기
        val cardList= mutableListOf<dataModel>()
        val itemKey= mutableListOf<String>()
        for( i in 0 until dataList.size){
            if(dataList[i].placeAddress==tag) {
                cardList.add(dataList[i])
                itemKey.add(itemsKeyList[i])
            }
        }
//        viewPager2.adapter=bannerAdapter(cardList)
        val vpAdapter=bannerAdapter(cardList)
        viewPager2.adapter=vpAdapter
        viewPager2.orientation=ViewPager2.ORIENTATION_HORIZONTAL
        springDotsIndicator.setViewPager2(viewPager2)

        //viewPager2 클릭 이벤트
        vpAdapter.setItemClickListener(object:bannerAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int) {
                val intent = Intent(context, DetailActivity::class.java)
                //firebase에 있는 board에 대한 데이터의 id를 가져오기
                intent.putExtra("key", itemKey[position])
                Log.d("keyitemkey", cardList.toString())
                Log.d("keyitem", itemKey.toString())
                Log.d("keyposition",position.toString())
                startActivity(intent)
            }

        })

    }
    private fun listViewAll(){
        val boardRef : DatabaseReference = database.getReference("map_contents")
        boardRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                items.clear()
                for (data in snapshot.children) {
                    val item = data.getValue(dataModel::class.java)
                    if (item != null) {
                        Log.d("category",category)

                        items.add(item!!)
                        itemsKeyList.add(data.key.toString())

                    }
                    itemsKeyList.reverse()
                    items.reverse()
                    //adapter.notifyDataSetChanged()
                }
                Log.d("bun1",items.toString())

            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
        Log.d("bun2",items.toString())

    }

}