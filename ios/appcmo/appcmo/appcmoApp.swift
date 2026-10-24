//
//  appcmoApp.swift
//  appcmo
//
//  Created by mauricioFK on 23/10/26.
//

import SwiftUI

@main
struct appcmoApp: App {
    @State private var store = SessionStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(store)
                .tint(Color.cmoMarca)
        }
    }
}
