package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boardgamegeek.R
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.compose.GeekListHeader
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import com.boardgamegeek.util.XmlApiMarkupConverter
import dagger.hilt.android.AndroidEntryPoint
import com.boardgamegeek.ui.compose.LoadingIndicator

@AndroidEntryPoint
class GeekListDescriptionFragment : Fragment(R.layout.fragment_geeklist_description) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val markupConverter = XmlApiMarkupConverter(requireContext())
        view.findViewById<ComposeView>(R.id.composeView).setContent {
            val vm: GeekListViewModel = viewModel()
            val gl = vm.geekList.observeAsState()
            BggAppTheme {
                if (gl.value?.status == Status.REFRESHING) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LoadingIndicator(
                            Modifier
                                .align(Alignment.Center)
                                .padding(dimensionResource(R.dimen.padding_extra))
                        )
                    }
                } else if (gl.value?.data != null) {
                    GeekListDescriptionContent(
                        gl.value?.data!!,
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                            vertical = dimensionResource(R.dimen.material_margin_vertical)
                        ),
                        markupConverter,
                    )
                }
            }
        }
    }
}

@Composable
fun GeekListDescriptionContent(geekList: GeekList, modifier: Modifier = Modifier, markupConverter: XmlApiMarkupConverter? = null) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState())
        ) {
            GeekListHeader(geekList, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Text(
                text = AnnotatedString.fromHtml(markupConverter?.toHtml(geekList.description) ?: geekList.description),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
