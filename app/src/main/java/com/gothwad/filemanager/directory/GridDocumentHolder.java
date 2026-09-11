package com.gothwad.manager.directory;

import android.content.Context;
import android.view.ViewGroup;

import com.gothwad.manager.R;
import com.gothwad.manager.common.RecyclerFragment.RecyclerItemClickListener.OnItemClickListener;
import com.gothwad.manager.directory.DocumentsAdapter.Environment;

public class GridDocumentHolder extends ListDocumentHolder {

    public GridDocumentHolder(Context context, ViewGroup parent,
                              OnItemClickListener onItemClickListener, Environment environment) {
        super(context, parent, R.layout.item_doc_grid, onItemClickListener, environment);
    }

}
