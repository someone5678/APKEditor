/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.apkeditor.info;

import com.reandroid.arsc.array.ResValueMapArray;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.container.SpecTypePair;
import com.reandroid.arsc.group.EntryGroup;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResTableMapEntry;
import com.reandroid.arsc.value.ResValue;
import com.reandroid.arsc.value.ResValueMap;
import com.reandroid.json.JSONWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

public class InfoWriterJson extends InfoWriter{
    private final JSONWriter mJsonWriter;
    public InfoWriterJson(Writer writer) {
        super(writer);
        JSONWriter jsonWriter = new JSONWriter(writer);
        jsonWriter = jsonWriter.array();
        this.mJsonWriter = jsonWriter;
    }

    @Override
    public void writeResources(PackageBlock packageBlock, List<String> typeFilters, boolean writeEntries) throws IOException {
        packageBlock.sortTypes();
        JSONWriter jsonWriter = mJsonWriter.object()
                .key("id").value(packageBlock.getId())
                .key("package").value(packageBlock.getName())
                .key("types").array();

        for(SpecTypePair specTypePair : packageBlock.listSpecTypePairs()){
            if(!contains(specTypePair, typeFilters)){
                continue;
            }
            writeResources(specTypePair, writeEntries);
        }
        jsonWriter.endArray()
                .endObject();
    }
    public void writeResources(SpecTypePair specTypePair, boolean writeEntries) throws IOException {
        JSONWriter jsonWriter = mJsonWriter.object()
                .key("id").value(specTypePair.getId())
                .key("type").value(specTypePair.getTypeName())
                .key("entries").array();

        List<EntryGroup> entryGroupList = toSortedEntryGroups(
                specTypePair.createEntryGroups(true).values());

        for(EntryGroup entryGroup : entryGroupList){
            writeResources(entryGroup, writeEntries);
        }
        jsonWriter.endArray().endObject();
    }
    @Override
    public void writeResources(EntryGroup entryGroup, boolean writeEntries) throws IOException {
        JSONWriter jsonWriter = mJsonWriter.object()
                .key("id").value(entryGroup.getResourceId())
                .key("type").value(entryGroup.getTypeName())
                .key("name").value(entryGroup.getSpecName());
        if(writeEntries){
            jsonWriter.key("configs");
            writeEntries(sortEntries(entryGroup.listItems()));
        }
        jsonWriter.endObject();
    }

    public void writeEntries(List<Entry> entryList) throws IOException {
        JSONWriter jsonWriter = mJsonWriter.array();
        for(Entry entry : entryList){
            writeEntry(entry);
        }
        jsonWriter.endArray();
    }
    public void writeEntry(Entry entry) throws IOException {
        if(entry.isComplex()){
            writeBagEntry(entry);
        }else {
            writeResEntry(entry);
        }
    }
    private void writeResEntry(Entry entry) throws IOException {
        ResValue resValue = entry.getResValue();
        if(resValue == null){
            return;
        }
        mJsonWriter.object()
                .key(NAME_QUALIFIERS).value(entry.getResConfig().getQualifiers())
                .key("value").value(getValueAsString(resValue))
                .endObject();
    }
    private void writeBagEntry(Entry entry) {
        ResValueMapArray mapArray = entry.getResValueMapArray();
        JSONWriter jsonWriter = mJsonWriter.object()
                .key(NAME_QUALIFIERS).value(entry.getResConfig().getQualifiers())
                .key("size").value(mapArray.childesCount())
                .key("parent").value(((ResTableMapEntry)entry.getTableEntry()).getParentId())
                .key(TAG_BAG).array();
        for(ResValueMap resValueMap : mapArray.getChildes()){
            writeValueMap(resValueMap);
        }
        jsonWriter.endArray()
                .endObject();
    }
    private void writeValueMap(ResValueMap resValueMap){
        mJsonWriter.object()
                .key("name").value(resValueMap.decodeName())
                .key("id").value(resValueMap.getNameResourceID())
                .key("value").value(getValueAsString(resValueMap))
                .endObject();
    }
    @Override
    public void writePackageNames(Collection<PackageBlock> packageBlocks) throws IOException {
        if(packageBlocks == null || packageBlocks.size() == 0){
            return;
        }
        JSONWriter jsonWriter = mJsonWriter.object()
                .key(TAG_RES_PACKAGES).array();

        for(PackageBlock packageBlock : packageBlocks){
            jsonWriter.object()
                    .key("id").value(packageBlock.getId())
                    .key("name").value(packageBlock.getName())
                    .endObject();
        }
        jsonWriter.endArray()
                .endObject();
    }
    @Override
    public void writeEntries(String name, List<Entry> entryList) throws IOException {
        if(entryList == null || entryList.size() == 0){
            return;
        }
        Entry first = entryList.get(0);
        JSONWriter jsonWriter = mJsonWriter.object()
                .key("id").value(first.getResourceId())
                .key("type").value(first.getTypeName())
                .key("name").value(first.getName())
                .key("entries")
                .array();

        for(Entry entry : entryList){
            jsonWriter.object()
                    .key("config").value(entry.getResConfig().getQualifiers())
                    .key("value").value(getValueAsString(entry))
                    .endObject();
        }
        jsonWriter.endArray()
                .endObject();
    }
    @Override
    public void writeArray(String name, Object[] values) throws IOException {

        JSONWriter jsonWriter = mJsonWriter.object()
                .key(name)
                .array();

        for(Object value:values){
            jsonWriter.value(value);
        }

        jsonWriter.endArray()
                .endObject();
    }
    @Override
    public void writeNameValue(String name, Object value) throws IOException {
        mJsonWriter.object()
                .key(name)
                .value(value)
                .endObject();
        getWriter().flush();
    }
    @Override
    public void flush() throws IOException {
        Writer writer = getWriter();
        mJsonWriter.endArray();
        writer.write("\n");
        writer.flush();
    }
}
